package com.github.t1.wunderbar.mock;

import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static com.github.t1.wunderbar.mock.GraphQLBodyMatcher.graphQlRequest;
import static com.github.t1.wunderbar.mock.GraphQLResponseSupplier.graphQL;
import static com.github.t1.wunderbar.mock.GraphQLResponseSupplier.graphQlError;
import static com.github.t1.wunderbar.mock.MockUtils.productQuery;
import static com.github.t1.wunderbar.mock.RequestMatcher.json;
import static com.github.t1.wunderbar.mock.RestErrorSupplier.restError;
import static com.github.t1.wunderbar.mock.RestResponseSupplier.restResponse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

@Slf4j
public class MockService {
    static final Map<RequestMatcher, ResponseSupplier> EXPECTATIONS = new LinkedHashMap<>();

    static {
        EXPECTATIONS.put(graphQlRequest().query(
                    "mutation addWunderBarExpectation\\(\\$matcher: RequestMatcherInput!, \\$responseSupplier: ResponseSupplierInput!\\) " +
                    "\\{ addWunderBarExpectation\\(matcher: \\$matcher, responseSupplier: \\$responseSupplier\\) }")
                .build(),
            MockService::addExpectation);
        EXPECTATIONS.put(productQuery("existing-product-id"),
            graphQL().add("data", Json.createObjectBuilder()
                .add("product", Json.createObjectBuilder()
                    .add("id", "existing-product-id")
                    .add("name", "some-product-name")
                    .add("price", 1599)
                )).build());
        EXPECTATIONS.put(productQuery("unknown-product-id"),
            graphQlError("product-not-found", "product unknown-product-id not found"));
        EXPECTATIONS.put(productQuery("unexpected-fail")
            , graphQlError("unexpected-fail", "product unexpected-fail fails unexpectedly"));
        EXPECTATIONS.put(graphQlRequest().emptyBody(true).build(),
            graphQlError("validation-error", "no body in GraphQL request"));

        EXPECTATIONS.put(RequestMatcher.builder().path("/rest/products/existing-product-id").build(), restResponse().body(Json.createObjectBuilder()
            .add("id", "existing-product-id")
            .add("name", "some-product-name")
            .add("price", 1599)
            .build()
        ));
        EXPECTATIONS.put(RequestMatcher.builder().path("/rest/products/forbidden-product-id").build(), restError().status(403)
            .detail("HTTP 403 Forbidden")
            .title("ForbiddenException")
            .type("urn:problem-type:javax.ws.rs.ForbiddenException")
        );
        EXPECTATIONS.put(RequestMatcher.builder().path("/rest/products/unknown-product-id").build(), restError().status(404)
            .detail("HTTP 404 Not Found")
            .title("NotFoundException")
            .type("urn:problem-type:javax.ws.rs.NotFoundException")
        );
        EXPECTATIONS.put(RequestMatcher.builder().path("/q/health/ready").build(), restResponse().add("status", "UP"));
    }

    private static void addExpectation(HttpServletRequest request, String requestBody, HttpServletResponse response) {
        var variables = json(requestBody).asJsonObject().getJsonObject("variables");
        log.debug("add expectation: {}", variables);
        var matcher = matcher(variables.getJsonObject("matcher"));
        log.debug("    matcher: {}", matcher);
        var supplier = supplier(variables.getJsonObject("responseSupplier"));
        log.debug("    supplier: {}", supplier);
        EXPECTATIONS.put(matcher, supplier);
        graphQL().with(builder -> builder.add("status", "ok")).build().apply(request, requestBody, response);
    }

    private static RequestMatcher matcher(JsonObject json) {
        var bodyMatcher = json.getJsonObject("bodyMatcher");
        if (hasField(bodyMatcher, "query")) return graphQlMatcher(bodyMatcher);
        if (hasField(json, "path")) return restMatcher(json);
        throw new IllegalArgumentException("need either `query` or `path`, but got " + json);
    }

    private static boolean hasField(JsonObject json, String query) {
        return json != null && json.getString(query, null) != null;
    }

    private static RequestMatcher graphQlMatcher(JsonObject json) {
        return graphQlRequest()
            .emptyBody(json.getBoolean("emptyBody"))
            .query(json.getString("query"))
            .variables(variables(json.getJsonArray("variables")))
            .build();
    }

    private static Map<String, Object> variables(JsonArray variables) {
        return variables.stream()
            .map(JsonValue::asJsonObject)
            .collect(toMap(o -> o.getString("key"), o -> o.getString("value")));
    }

    private static RequestMatcher restMatcher(JsonObject json) {
        return null; // TODO
    }

    private static ResponseSupplier supplier(JsonObject json) {
        return new GraphQLResponseSupplier(json.getString("body"));
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("received {}:{}", request.getMethod(), request.getRequestURI());
        var body = request.getReader().lines().collect(joining()).trim();
        log.debug("received body:\n{}", body); // TODO trace
        var match = EXPECTATIONS.entrySet().stream()
            .peek(entry -> log.debug("match {}", entry.getKey())) // TODO trace
            .filter(entry -> entry.getKey().matches(request, body))
            .peek(entry -> log.debug("matched!")) // TODO trace
            .findFirst().orElseGet(MockService::noMatch);
        match.getValue().apply(request, body, response);
    }

    private static Entry<RequestMatcher, ResponseSupplier> noMatch() {
        var message = "no matching expectation found";
        log.debug(message);
        return Map.entry(RequestMatcher.builder().build(), restError().detail(message));
    }
}
