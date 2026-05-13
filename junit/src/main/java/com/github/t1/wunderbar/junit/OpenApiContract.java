package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.http.Authorization;
import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.t1.wunderbar.http.HttpUtils.formatJson;
import static com.github.t1.wunderbar.http.HttpUtils.isCompatible;
import static com.github.t1.wunderbar.http.HttpUtils.readJson;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static java.util.Locale.ROOT;

@Slf4j
public @Internal record OpenApiContract(String openapi, List<TestCase> tests) {
    public static OpenApiContract from(String json) {
        var root = readJson(json).asJsonObject();
        var paths = Optional.ofNullable(root.getJsonObject("paths"))
                .orElseThrow(() -> new WunderBarException("OpenAPI file doesn't contain paths"));
        var globalSecurity = root.getJsonArray("security");
        var securitySchemes = securitySchemes(root);

        return new OpenApiContract(root.getString("openapi", null), testsFrom(paths, globalSecurity, securitySchemes));
    }

    public static String toJson(List<TestCase> tests, String version) {
        warnAboutLosses(tests);
        var exportableTests = tests.stream().filter(OpenApiContract::isExportable).toList();

        var root = Json.createObjectBuilder()
                .add("openapi", "3.1.0")
                .add("info", Json.createObjectBuilder()
                        .add("title", "WunderBar contract")
                        .add("version", version))
                .add("paths", buildPaths(exportableTests));
        var securitySchemes = buildSecuritySchemes(exportableTests);
        if (!securitySchemes.isEmpty())
            root.add("components", Json.createObjectBuilder().add("securitySchemes", securitySchemes));
        return formatJson(root.build());
    }

    public String displayName(Path path) {
        var version = (openapi == null || openapi.isBlank()) ? "OpenAPI" : "OpenAPI " + openapi;
        return path.getFileName() + " [" + version + "]";
    }

    public Optional<TestCase> test(Path path) {
        return tests.stream().filter(test -> Path.of(test.path()).equals(path)).findFirst();
    }

    public static String versionFromComment(String comment) {
        if (comment == null) return "1.1";
        var matcher = VERSION.matcher(comment);
        return matcher.find() ? matcher.group("version") : "1.1";
    }

    private static List<TestCase> testsFrom(JsonObject paths, JsonArray globalSecurity, Map<String, AuthorizationType> securitySchemes) {
        var tests = new ArrayList<TestCase>();
        paths.forEach((path, pathItem) -> {
            if (path.contains("{"))
                throw new WunderBarException("OpenAPI import supports only concrete paths, but found template path " + path);
            if (!(pathItem instanceof JsonObject operationMap)) return;
            for (var method : HTTP_METHODS) {
                if (!operationMap.containsKey(method)) continue;
                tests.addAll(testsFromOperation(path, method, operationMap.getJsonObject(method), globalSecurity, securitySchemes));
            }
        });
        return tests;
    }

    private static List<TestCase> testsFromOperation(
            String path,
            String method,
            JsonObject operation,
            JsonArray globalSecurity,
            Map<String, AuthorizationType> securitySchemes
    ) {
        var operationName = operation.getString("operationId", operationId(new OperationKey(path, method)));
        var security = operation.containsKey("security") ? operation.getJsonArray("security") : globalSecurity;
        var authorization = authorization(operationName, security, securitySchemes);
        var responseExamples = responseExamples(operationName, operation.getJsonObject("responses"));
        var requestExamples = requestExamples(operationName, operation.getJsonObject("requestBody"));

        if (requestExamples.isEmpty())
            return testsWithNoRequestBody(operationName, method, path, authorization, responseExamples);
        if (requestExamples.size() == 1)
            return testsWithSingleRequest(operationName, method, path, authorization, requestExamples.getFirst(), responseExamples);
        return testsWithMatchedExamples(operationName, method, path, authorization, requestExamples, responseExamples);
    }

    private static List<TestCase> testsWithNoRequestBody(
            String operationName, String method, String path, Authorization authorization, List<ResponseExample> responseExamples
    ) {
        return responseExamples.stream()
                .map(response -> new TestCase(testPath(operationName, response.name()), List.of(new HttpInteraction(1,
                        HttpRequest.builder().method(method.toUpperCase(ROOT)).uri(path)
                                .authorization(authorization)
                                .accept(accept(response.mediaType()))
                                .build(),
                        response.toResponse()))))
                .toList();
    }

    private static List<TestCase> testsWithSingleRequest(
            String operationName, String method, String path, Authorization authorization,
            BodyExample req, List<ResponseExample> responseExamples
    ) {
        return responseExamples.stream()
                .map(response -> new TestCase(testPath(operationName, response.name()), List.of(new HttpInteraction(1,
                        HttpRequest.builder().method(method.toUpperCase(ROOT)).uri(path)
                                .authorization(authorization)
                                .contentType(req.mediaType())
                                .accept(accept(response.mediaType()))
                                .body(req.body())
                                .build(),
                        response.toResponse()))))
                .toList();
    }

    private static List<TestCase> testsWithMatchedExamples(
            String operationName, String method, String path, Authorization authorization,
            List<BodyExample> requestExamples, List<ResponseExample> responseExamples
    ) {
        var requestsByName = byName(requestExamples);
        var responsesByName = byName(responseExamples);
        if (!requestsByName.keySet().equals(responsesByName.keySet()))
            throw new WunderBarException("OpenAPI import requires matching request/response example names for operation " + operationName);
        return requestsByName.keySet().stream()
                .map(name -> new TestCase(testPath(operationName, name), List.of(new HttpInteraction(1,
                        HttpRequest.builder().method(method.toUpperCase(ROOT)).uri(path)
                                .authorization(authorization)
                                .contentType(requestsByName.get(name).mediaType())
                                .accept(accept(responsesByName.get(name).mediaType()))
                                .body(requestsByName.get(name).body())
                                .build(),
                        responsesByName.get(name).toResponse()))))
                .toList();
    }

    private static <T extends NamedExample> Map<String, T> byName(List<T> examples) {
        var map = new LinkedHashMap<String, T>();
        for (var example : examples) {
            if (map.put(example.name(), example) != null)
                throw new WunderBarException("duplicate OpenAPI example name " + example.name());
        }
        return map;
    }

    private static List<BodyExample> requestExamples(String operationName, JsonObject requestBody) {
        if (requestBody == null) return List.of();
        var content = Optional.ofNullable(requestBody.getJsonObject("content"))
                .orElseThrow(() -> new WunderBarException("OpenAPI import requires requestBody.content for operation " + operationName));
        return examples(operationName, "request", content);
    }

    private static List<ResponseExample> responseExamples(String operationName, JsonObject responses) {
        if (responses == null || responses.isEmpty())
            throw new WunderBarException("OpenAPI import requires responses for operation " + operationName);

        var out = new ArrayList<ResponseExample>();
        responses.forEach((status, responseValue) -> {
            if (!(responseValue instanceof JsonObject response)) return;
            var content = response.getJsonObject("content");
            if (content == null || content.isEmpty()) {
                out.add(new ResponseExample(status + "-response", Integer.parseInt(status), APPLICATION_JSON, null));
                return;
            }
            examples(operationName, status + " response", content).forEach(example -> out.add(new ResponseExample(
                    uniqueName(example.name(), status, out), Integer.parseInt(status), example.mediaType(), example.body())));
        });
        if (out.isEmpty()) throw new WunderBarException("OpenAPI import requires response examples for operation " + operationName);
        return out;
    }

    private static String uniqueName(String name, String status, List<? extends NamedExample> existing) {
        return existing.stream().anyMatch(example -> example.name().equals(name)) ? name + "-" + status : name;
    }

    private static List<BodyExample> examples(String operationName, String location, JsonObject content) {
        var out = new ArrayList<BodyExample>();
        content.forEach((mediaType, mediaTypeValue) -> {
            if (!(mediaTypeValue instanceof JsonObject mediaTypeObject)) return;
            if (mediaTypeObject.containsKey("examples")) {
                mediaTypeObject.getJsonObject("examples").forEach((name, exampleValue) ->
                        out.add(new BodyExample(name, mediaType, parseBody(mediaType, exampleValue))));
                return;
            }
            if (mediaTypeObject.containsKey("example")) {
                out.add(new BodyExample(operationName, mediaType, parseBody(mediaType, mediaTypeObject.get("example"))));
            }
        });
        if (out.isEmpty())
            throw new WunderBarException("OpenAPI import requires explicit " + location + " examples for operation " + operationName);
        return out;
    }

    private static String parseBody(String mediaType, JsonValue exampleValue) {
        var value = (exampleValue instanceof JsonObject exampleObject && exampleObject.containsKey("value"))
                ? exampleObject.get("value") : exampleValue;
        if (isCompatible(APPLICATION_JSON_TYPE, MediaType.valueOf(mediaType))) return formatJson(value);
        return (value instanceof JsonString jsonString) ? jsonString.getString() : value.toString();
    }

    private static Authorization authorization(String operationName, JsonArray security, Map<String, AuthorizationType> securitySchemes) {
        if (security == null || security.isEmpty()) return null;

        var refs = security.stream()
                .filter(v -> v instanceof JsonObject)
                .flatMap(v -> ((JsonObject) v).keySet().stream())
                .distinct()
                .toList();
        if (refs.isEmpty()) return null;
        if (refs.size() > 1)
            throw new WunderBarException("OpenAPI import supports only one security scheme per operation: " + operationName);

        var type = securitySchemes.get(refs.getFirst());
        if (type == null)
            throw new WunderBarException("OpenAPI import references unknown security scheme " + refs.getFirst() + " on operation " + operationName);
        return type.toAuthorization();
    }

    private static Map<String, AuthorizationType> securitySchemes(JsonObject root) {
        var out = new LinkedHashMap<String, AuthorizationType>();
        var components = root.getJsonObject("components");
        if (components == null) return out;
        var securitySchemes = components.getJsonObject("securitySchemes");
        if (securitySchemes == null) return out;
        securitySchemes.forEach((name, schemeValue) -> {
            if (!(schemeValue instanceof JsonObject scheme)) return;
            var type = scheme.getString("type", "");
            var httpScheme = scheme.getString("scheme", "").toLowerCase(ROOT);
            if (!"http".equals(type)) return;
            switch (httpScheme) {
                case "basic" -> out.put(name, AuthorizationType.BASIC);
                case "bearer" -> out.put(name, AuthorizationType.BEARER);
                default -> log.warn("ignore unsupported OpenAPI security scheme {} {}", type, httpScheme);
            }
        });
        return out;
    }

    private static void warnAboutLosses(List<TestCase> tests) {
        tests.stream().filter(test -> !isExportable(test))
                .forEach(test -> log.warn("OpenAPI export skips multi-interaction test {} because OpenAPI support is limited to simple single-interaction cases", test.path()));
        interactionsWithPath(tests)
                .filter(entry -> entry.getValue().getRequest().getHeaders() != null)
                .filter(entry -> entry.getValue().getRequest().getHeaders().stream().anyMatch(OpenApiContract::isCustomHeader))
                .forEach(entry -> log.warn("OpenAPI export drops custom request headers for {}", entry.getKey()));
        interactionsWithPath(tests)
                .filter(entry -> entry.getValue().getRequest().getAuthorization() != null)
                .forEach(entry -> log.warn("OpenAPI export loses authorization identity details for {}", entry.getKey()));
    }

    private static Stream<Map.Entry<String, HttpInteraction>> interactionsWithPath(List<TestCase> tests) {
        return tests.stream().flatMap(test -> test.interactions().stream().map(interaction -> Map.entry(test.path(), interaction)));
    }

    private static JsonObjectBuilder buildPaths(List<TestCase> tests) {
        var paths = Json.createObjectBuilder();
        var operations = new LinkedHashMap<OperationKey, List<Example>>();

        for (var test : tests) {
            for (var interaction : test.interactions()) {
                var request = interaction.getRequest();
                var path = request.getUri().getPath();
                if (path == null || path.isBlank()) path = "/";
                var operationKey = new OperationKey(path, request.getMethod().toLowerCase(ROOT));
                operations.computeIfAbsent(operationKey, __ -> new ArrayList<>())
                        .add(new Example(exampleName(test.path(), interaction.getNumber()), interaction));
            }
        }

        var groupedByPath = new LinkedHashMap<String, JsonObjectBuilder>();
        for (var entry : operations.entrySet()) {
            groupedByPath.computeIfAbsent(entry.getKey().path, __ -> Json.createObjectBuilder())
                    .add(entry.getKey().method, buildOperation(entry.getKey(), entry.getValue()));
        }
        groupedByPath.forEach(paths::add);
        return paths;
    }

    private static JsonObject buildSecuritySchemes(List<TestCase> tests) {
        var types = tests.stream()
                .flatMap(test -> test.interactions().stream())
                .map(HttpInteraction::getRequest)
                .map(HttpRequest::getAuthorization)
                .filter(Objects::nonNull)
                .map(AuthorizationType::of)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var builder = Json.createObjectBuilder();
        if (types.contains(AuthorizationType.BASIC))
            builder.add(AuthorizationType.BASIC.schemeName(), Json.createObjectBuilder().add("type", "http").add("scheme", "basic"));
        if (types.contains(AuthorizationType.BEARER))
            builder.add(AuthorizationType.BEARER.schemeName(), Json.createObjectBuilder().add("type", "http").add("scheme", "bearer"));
        return builder.build();
    }

    private static JsonObjectBuilder buildOperation(OperationKey key, List<Example> examples) {
        var builder = Json.createObjectBuilder()
                .add("operationId", operationId(key))
                .add("responses", buildResponses(examples));

        var requestBody = buildRequestBody(examples);
        if (requestBody != null) builder.add("requestBody", requestBody);

        var security = buildSecurity(examples, key);
        if (security != null) builder.add("security", security);

        return builder;
    }

    private static JsonArray buildSecurity(List<Example> examples, OperationKey key) {
        var authTypes = examples.stream()
                .map(example -> example.interaction.getRequest().getAuthorization())
                .map(auth -> (auth == null) ? AuthorizationType.NONE : AuthorizationType.of(auth))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (authTypes.isEmpty() || authTypes.equals(Set.of(AuthorizationType.NONE))) return null;
        if (authTypes.size() > 1) {
            log.warn("OpenAPI export can't faithfully represent mixed authorization requirements for {} {}", key.method, key.path);
            return null;
        }
        var type = authTypes.getFirst();
        if (type == AuthorizationType.NONE) return null;
        return Json.createArrayBuilder().add(Json.createObjectBuilder().add(type.schemeName(), Json.createArrayBuilder())).build();
    }

    private static JsonObjectBuilder buildRequestBody(List<Example> examples) {
        var examplesWithBody = examples.stream().filter(example -> example.interaction.getRequest().hasBody()).toList();
        if (examplesWithBody.isEmpty()) return null;

        return Json.createObjectBuilder()
                .add("required", true)
                .add("content", buildContent(examplesWithBody, Direction.REQUEST));
    }

    private static JsonObjectBuilder buildResponses(List<Example> examples) {
        var responses = Json.createObjectBuilder();
        var byStatus = new LinkedHashMap<String, List<Example>>();
        for (var example : examples) {
            var status = Integer.toString(example.interaction.getResponse().getStatusCode());
            byStatus.computeIfAbsent(status, __ -> new ArrayList<>()).add(example);
        }

        byStatus.forEach((status, statusExamples) -> {
            var firstResponse = statusExamples.getFirst().interaction.getResponse();
            var response = Json.createObjectBuilder().add("description", firstResponse.getStatusString());
            if (statusExamples.stream().anyMatch(example -> example.interaction.getResponse().hasBody()))
                response.add("content", buildContent(statusExamples, Direction.RESPONSE));
            responses.add(status, response);
        });
        return responses;
    }

    private static JsonObjectBuilder buildContent(List<Example> examples, Direction direction) {
        var content = Json.createObjectBuilder();
        var byMediaType = new LinkedHashMap<String, List<Example>>();

        for (var example : examples) {
            var mediaType = mediaType(example, direction).toString();
            byMediaType.computeIfAbsent(mediaType, __ -> new ArrayList<>()).add(example);
        }

        byMediaType.forEach((mediaType, mediaTypeExamples) -> {
            var mediaTypeObject = Json.createObjectBuilder();
            var examplesObject = Json.createObjectBuilder();
            for (var example : mediaTypeExamples) {
                body(example, direction).ifPresent(body -> examplesObject.add(example.name, Json.createObjectBuilder()
                        .add("value", exampleValue(body, mediaType(example, direction)))));
            }
            mediaTypeObject.add("examples", examplesObject);
            content.add(mediaType, mediaTypeObject);
        });

        return content;
    }

    private static MediaType mediaType(Example example, Direction direction) {
        return switch (direction) {
            case REQUEST -> example.interaction.getRequest().getContentType();
            case RESPONSE -> example.interaction.getResponse().getContentType();
        };
    }

    private static Optional<String> body(Example example, Direction direction) {
        return switch (direction) {
            case REQUEST -> example.interaction.getRequest().body();
            case RESPONSE -> example.interaction.getResponse().body();
        };
    }

    private static JsonValue exampleValue(String body, MediaType mediaType) {
        return isCompatible(APPLICATION_JSON_TYPE, mediaType) ? readJson(body) : Json.createValue(body);
    }

    private static String operationId(OperationKey key) {
        return (key.method + "_" + key.path)
                .replaceAll("[^a-zA-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String exampleName(String testPath, int interactionNumber) {return testPath + "#" + interactionNumber;}

    private static String testPath(String operationName, String exampleName) {
        if (exampleName == null || exampleName.isBlank()) return operationName;
        return exampleName.replaceFirst("#1$", "");
    }

    public record TestCase(String path, List<HttpInteraction> interactions) {}

    private interface NamedExample {String name();}

    private record BodyExample(String name, String mediaType, String body) implements NamedExample {}

    private record ResponseExample(String name, int statusCode, String mediaType, String body) implements NamedExample {
        private HttpResponse toResponse() {
            var builder = HttpResponse.builder().status(statusCode).contentType(mediaType);
            if (body != null) builder.body(body);
            return builder.build();
        }
    }

    private enum Direction {REQUEST, RESPONSE}

    private record Example(String name, HttpInteraction interaction) {}

    private record OperationKey(String path, String method) {}

    private enum AuthorizationType {
        NONE {
            @Override Authorization toAuthorization() {return null;}

            @Override String schemeName() {throw new IllegalStateException("NONE has no scheme name");}
        },
        BASIC {
            @Override Authorization toAuthorization() {return new Authorization.Dummy("openapi-basic-user");}

            @Override String schemeName() {return "basicAuth";}
        },
        BEARER {
            @Override Authorization toAuthorization() {return new Authorization.Dummy("openapi-bearer-user");}

            @Override String schemeName() {return "bearerAuth";}
        };

        static AuthorizationType of(Authorization authorization) {
            if (authorization == null) return NONE;
            if (authorization instanceof Authorization.Basic) return BASIC;
            if (authorization instanceof Authorization.Bearer || authorization instanceof Authorization.Dummy) return BEARER;
            throw new IllegalArgumentException("unsupported authorization type " + authorization.getClass().getName());
        }

        abstract Authorization toAuthorization();

        abstract String schemeName();
    }

    private static boolean isExportable(TestCase test) {return test.interactions().size() == 1;}

    private static String accept(String responseMediaType) {
        return isCompatible(APPLICATION_JSON_TYPE, MediaType.valueOf(responseMediaType)) ? APPLICATION_JSON : responseMediaType;
    }

    private static boolean isCustomHeader(HttpRequest.Header header) {
        return switch (header.name()) {
            case "Accept-Encoding", "Connection", "Host", "User-Agent" -> false;
            default -> true;
        };
    }

    private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "patch", "delete", "head", "options");
    private static final Pattern VERSION = Pattern.compile("(?m)^version:\\s*(?<version>\\S+)");
}
