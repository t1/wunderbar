package com.github.t1.wunderbar.mock;

import com.github.t1.wunderbar.mock.GraphQLBodyMatcher.GraphQLBodyMatcherBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.mock.GraphQLBodyMatcher.graphQlRequest;
import static com.github.t1.wunderbar.mock.GraphQLResponseSupplier.graphQL;
import static com.github.t1.wunderbar.mock.GraphQLResponseSupplier.graphQlError;
import static com.github.t1.wunderbar.mock.MockUtils.productQuery;
import static com.github.t1.wunderbar.mock.RequestMatcher.json;
import static com.github.t1.wunderbar.mock.RestErrorSupplier.restError;
import static com.github.t1.wunderbar.mock.RestResponseSupplier.restResponse;
import static com.github.t1.wunderbar.mock.WunderBarMockExpectation.exp;
import static java.util.stream.Collectors.joining;

@Slf4j
public class MockService {
    static final List<WunderBarMockExpectation> EXPECTATIONS = new ArrayList<>();

    static {
        addExpectation(graphQlRequest().queryPattern(
                "mutation addWunderBarExpectation\\(\\$matcher: RequestMatcherInput!, \\$responseSupplier: ResponseSupplierInput!\\) " +
                "\\{ addWunderBarExpectation\\(matcher: \\$matcher, responseSupplier: \\$responseSupplier\\) \\{id status} }"),
            MockService::addExpectation);
        addExpectation(graphQlRequest().queryPattern(
                "mutation removeWunderBarExpectation\\(\\$id: Int!\\) \\{ removeWunderBarExpectation\\(id: \\$id\\) }"),
            MockService::removeExpectation);

        addExpectation(productQuery("existing-product-id"),
            graphQL().add("data", Json.createObjectBuilder()
                .add("product", Json.createObjectBuilder()
                    .add("id", "existing-product-id")
                    .add("name", "some-product-name")
                    .add("price", 1599)
                )).build());
        addExpectation(productQuery("unexpected-fail")
            , graphQlError("unexpected-fail", "product unexpected-fail fails unexpectedly"));
        addExpectation(graphQlRequest(), graphQlError("validation-error", "no body in GraphQL request"));

        addExpectation(RequestMatcher.builder().path("/rest/products/existing-product-id").build(), restResponse().body(Json.createObjectBuilder()
            .add("id", "existing-product-id")
            .add("name", "some-product-name")
            .add("price", 1599)
            .build()
        ));
        addExpectation(RequestMatcher.builder().path("/rest/products/forbidden-product-id").build(), restError().status(403)
            .detail("HTTP 403 Forbidden")
            .title("ForbiddenException")
            .type("urn:problem-type:javax.ws.rs.ForbiddenException")
        );
        addExpectation(RequestMatcher.builder().path("/rest/products/unknown-product-id").build(), restError().status(404)
            .detail("HTTP 404 Not Found")
            .title("NotFoundException")
            .type("urn:problem-type:javax.ws.rs.NotFoundException")
        );
        addExpectation(RequestMatcher.builder().path("/q/health/ready").build(), restResponse().add("status", "UP"));
    }

    private static void addExpectation(GraphQLBodyMatcherBuilder matcherBuilder, ResponseSupplier responseSupplier) {
        addExpectation(matcherBuilder.build(), responseSupplier);
    }

    private static WunderBarMockExpectation addExpectation(RequestMatcher queryMatcher, ResponseSupplier responseSupplier) {
        var expectation = exp(queryMatcher, responseSupplier);
        EXPECTATIONS.add(expectation);
        return expectation;
    }

    private static void addExpectation(HttpServletRequest request, String requestBody, HttpServletResponse response) {
        var variables = json(requestBody).asJsonObject().getJsonObject("variables");
        log.debug("add expectation: {}", variables);
        var matcher = matcher(variables.getJsonObject("matcher"));
        log.debug("    matcher: {}", matcher);
        var supplier = supplier(variables.getJsonObject("responseSupplier"));
        log.debug("    supplier: {}", supplier);
        var expectation = addExpectation(matcher, supplier);
        graphQL().with(builder -> expectationResponse(builder, expectation)).build()
            .apply(request, requestBody, response);
    }

    private static RequestMatcher matcher(JsonObject json) {
        var bodyMatcher = json.getJsonObject("bodyMatcher");
        if (hasField(bodyMatcher, "queryPattern") || hasField(bodyMatcher, "query")) return graphQlMatcher(bodyMatcher);
        if (hasField(json, "path")) return restMatcher(json);
        throw new IllegalArgumentException("need either `query` or `path`, but got " + json);
    }

    private static boolean hasField(JsonObject json, String query) {
        return json != null && json.getString(query, null) != null;
    }

    private static RequestMatcher graphQlMatcher(JsonObject json) {
        return graphQlRequest()
            .queryPattern(json.getString("queryPattern", null))
            .query(json.getString("query", null))
            .variables(json.getJsonObject("variables"))
            .build();
    }

    private static void expectationResponse(JsonObjectBuilder builder, WunderBarMockExpectation expectation) {
        builder.add("data", Json.createObjectBuilder()
            .add("addWunderBarExpectation", Json.createObjectBuilder()
                .add("status", "ok")
                .add("id", expectation.getId())));
    }

    private static RequestMatcher restMatcher(JsonObject json) {
        return null; // TODO
    }

    private static ResponseSupplier supplier(JsonObject json) {
        return new GraphQLResponseSupplier(json.getString("body"));
    }

    private static void removeExpectation(HttpServletRequest request, String requestBody, HttpServletResponse response) {
        var variables = json(requestBody).asJsonObject().getJsonObject("variables");
        log.debug("remove expectation: {}", variables);
        var id = variables.getInt("id");
        var expectation = EXPECTATIONS.stream().filter(e -> e.getId() == id)
            .findFirst().orElseThrow(() -> new IllegalStateException("can't remove expectation " + id + ": not found."));
        graphQL().with(MockService::removeResponse).build().apply(request, requestBody, response);
    }

    private static void removeResponse(JsonObjectBuilder builder) {
        builder.add("data", Json.createObjectBuilder()
            .add("removeWunderBarExpectation", "ok"));
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("received {}:{}", request.getMethod(), request.getRequestURI());
        var body = request.getReader().lines().collect(joining()).trim();
        log.debug("received body:\n{}", body); // TODO trace
        var match = EXPECTATIONS.stream()
            .peek(expectation -> log.debug("match {}", expectation.getRequestMatcher())) // TODO trace
            .filter(expectation -> expectation.getRequestMatcher().matches(request, body))
            .peek(expectation -> log.debug("matched!")) // TODO trace
            .findFirst().orElseGet(MockService::noMatch);
        match.getResponseSupplier().apply(request, body, response);
    }

    private static WunderBarMockExpectation noMatch() {
        var message = "no matching expectation found";
        log.debug(message);
        return exp(RequestMatcher.builder().build(), restError().detail(message));
    }
}
