package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.http.Authorization;
import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.MediaType;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.http.HttpUtils.formatJson;
import static com.github.t1.wunderbar.http.HttpUtils.isCompatible;
import static com.github.t1.wunderbar.http.HttpUtils.readJson;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static java.util.Locale.ROOT;

public @Internal record OpenApiContract(String openapi, List<TestCase> tests) {
    public static OpenApiContract from(String json) {
        var root = readJson(json).asJsonObject();
        var tests = Optional.ofNullable(root.getJsonArray("x-wunderbar-tests"))
                .orElseThrow(() -> new WunderBarException("OpenAPI file doesn't contain x-wunderbar-tests"));
        return new OpenApiContract(root.getString("openapi", null), tests.stream()
                .map(JsonValue::asJsonObject)
                .map(OpenApiContract::toTestCase)
                .toList());
    }

    public static String toJson(List<TestCase> tests, String version) {
        var root = Json.createObjectBuilder()
                .add("openapi", "3.1.0")
                .add("info", Json.createObjectBuilder()
                        .add("title", "WunderBar contract")
                        .add("version", version))
                .add("paths", buildPaths(tests))
                .add("x-wunderbar-tests", toJsonTests(tests));
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

    private static JsonArrayBuilder toJsonTests(List<TestCase> tests) {
        var out = Json.createArrayBuilder();
        tests.forEach(test -> out.add(Json.createObjectBuilder()
                .add("path", test.path)
                .add("interactions", toJsonInteractions(test.interactions))));
        return out;
    }

    private static JsonArrayBuilder toJsonInteractions(List<HttpInteraction> interactions) {
        var out = Json.createArrayBuilder();
        interactions.forEach(interaction -> out.add(Json.createObjectBuilder()
                .add("number", interaction.getNumber())
                .add("request", toJson(interaction.getRequest()))
                .add("response", toJson(interaction.getResponse()))));
        return out;
    }

    private static JsonObjectBuilder toJson(HttpRequest request) {
        var out = Json.createObjectBuilder()
                .add("method", request.getMethod())
                .add("uri", request.uri())
                .add("contentType", request.getContentType().toString())
                .add("accept", toJsonMediaTypes(request.getAccept()));
        if (request.getAuthorization() != null) out.add("authorization", request.getAuthorization().toDummy().toString());
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) out.add("headers", toJsonHeaders(request.getHeaders()));
        request.body().ifPresent(body -> out.add("body", body));
        return out;
    }

    private static JsonObjectBuilder toJson(HttpResponse response) {
        var out = Json.createObjectBuilder()
                .add("statusCode", response.getStatusCode())
                .add("contentType", response.getContentType().toString());
        response.body().ifPresent(body -> out.add("body", body));
        return out;
    }

    private static JsonArrayBuilder toJsonMediaTypes(List<MediaType> mediaTypes) {
        var out = Json.createArrayBuilder();
        mediaTypes.forEach(mediaType -> out.add(mediaType.toString()));
        return out;
    }

    private static JsonArrayBuilder toJsonHeaders(List<HttpRequest.Header> headers) {
        var out = Json.createArrayBuilder();
        headers.forEach(header -> out.add(Json.createObjectBuilder()
                .add("name", header.name())
                .add("values", toJsonStrings(header.values()))));
        return out;
    }

    private static JsonArrayBuilder toJsonStrings(List<String> values) {
        var out = Json.createArrayBuilder();
        values.forEach(out::add);
        return out;
    }

    private static TestCase toTestCase(JsonObject json) {
        return new TestCase(
                json.getString("path"),
                json.getJsonArray("interactions").stream()
                        .map(JsonValue::asJsonObject)
                        .map(OpenApiContract::toInteraction)
                        .toList());
    }

    private static HttpInteraction toInteraction(JsonObject json) {
        return new HttpInteraction(
                json.getInt("number"),
                toRequest(json.getJsonObject("request")),
                toResponse(json.getJsonObject("response")));
    }

    private static HttpRequest toRequest(JsonObject json) {
        var builder = HttpRequest.builder()
                .method(json.getString("method", "GET"))
                .uri(json.getString("uri", "/"))
                .contentType(json.getString("contentType", MediaType.APPLICATION_JSON));
        toStrings(json.getJsonArray("accept")).forEach(builder::accept);
        if (json.containsKey("authorization")) builder.authorization(Authorization.valueOf(json.getString("authorization")));
        if (json.containsKey("headers")) {
            for (var header : json.getJsonArray("headers").stream().map(JsonValue::asJsonObject).toList())
                builder.header(header.getString("name"), toStrings(header.getJsonArray("values")));
        }
        if (json.containsKey("body")) builder.body(json.getString("body"));
        return builder.build();
    }

    private static HttpResponse toResponse(JsonObject json) {
        var builder = HttpResponse.builder()
                .status(json.getInt("statusCode", 200))
                .contentType(json.getString("contentType", MediaType.APPLICATION_JSON));
        if (json.containsKey("body")) builder.body(json.getString("body"));
        return builder.build();
    }

    private static List<String> toStrings(JsonArray array) {
        if (array == null) return List.of();
        return array.stream().map(JsonString.class::cast).map(JsonString::getString).toList();
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
                operations.computeIfAbsent(operationKey, __ -> new java.util.ArrayList<>())
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

    private static JsonObjectBuilder buildOperation(OperationKey key, List<Example> examples) {
        var builder = Json.createObjectBuilder()
                .add("operationId", operationId(key))
                .add("responses", buildResponses(examples));

        var requestBody = buildRequestBody(examples);
        if (requestBody != null) builder.add("requestBody", requestBody);

        return builder;
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
            byStatus.computeIfAbsent(status, __ -> new java.util.ArrayList<>()).add(example);
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
            byMediaType.computeIfAbsent(mediaType, __ -> new java.util.ArrayList<>()).add(example);
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

    private static String exampleName(String testPath, int interactionNumber) {
        return testPath + "#" + interactionNumber;
    }

    public record TestCase(String path, List<HttpInteraction> interactions) {}

    private enum Direction {REQUEST, RESPONSE}

    private record Example(String name, HttpInteraction interaction) {}

    private record OperationKey(String path, String method) {}

    private static final Pattern VERSION = Pattern.compile("(?m)^version:\\s*(?<version>[^\\s]+)");
}
