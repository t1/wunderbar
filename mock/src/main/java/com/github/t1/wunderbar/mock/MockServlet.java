package com.github.t1.wunderbar.mock;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.mock.GraphQLResponseSupplier.graphQL;
import static com.github.t1.wunderbar.mock.GraphQLResponseSupplier.graphQlError;
import static com.github.t1.wunderbar.mock.RequestMatcher.graphQlRequest;
import static com.github.t1.wunderbar.mock.RequestMatcher.restRequest;
import static com.github.t1.wunderbar.mock.RestErrorSupplier.restError;
import static com.github.t1.wunderbar.mock.RestResponseSupplier.restResponse;
import static java.util.stream.Collectors.joining;

@Slf4j
@WebServlet(name = "WunderBar-Mock-Servlet", urlPatterns = {"/*"})
public class MockServlet extends HttpServlet {
    private static final Map<RequestMatcher, ResponseSupplier> EXPECTATIONS = new LinkedHashMap<>();

    static {
        EXPECTATIONS.put(productQuery("existing-product-id"),
            graphQL().add("data", Json.createObjectBuilder()
                .add("product", Json.createObjectBuilder()
                    .add("id", "existing-product-id")
                    .add("name", "some-product-name")
                    .add("price", 1599)
                )));
        EXPECTATIONS.put(productQuery("forbidden-product-id"),
            graphQlError("product-forbidden", "product forbidden-product-id is forbidden"));
        EXPECTATIONS.put(productQuery("unknown-product-id"),
            graphQlError("product-not-found", "product unknown-product-id not found"));
        EXPECTATIONS.put(productQuery("unexpected-fail")
            , graphQlError("unexpected-fail", "product unexpected-fail fails unexpectedly"));
        EXPECTATIONS.put(graphQlRequest().emptyBody(true).build(),
            graphQlError("validation-error", "no body in GraphQL request"));

        EXPECTATIONS.put(restRequest().path("/rest/products/existing-product-id").build(), restResponse().body(Json.createObjectBuilder()
            .add("id", "existing-product-id")
            .add("name", "some-product-name")
            .add("price", 1599)
            .build()
        ));
        EXPECTATIONS.put(restRequest().path("/rest/products/forbidden-product-id").build(), restError().status(403)
            .detail("HTTP 403 Forbidden")
            .title("ForbiddenException")
            .type("urn:problem-type:javax.ws.rs.ForbiddenException")
        );
        EXPECTATIONS.put(restRequest().path("/rest/products/unknown-product-id").build(), restError().status(404)
            .detail("HTTP 404 Not Found")
            .title("NotFoundException")
            .type("urn:problem-type:javax.ws.rs.NotFoundException")
        );
        EXPECTATIONS.put(restRequest().path("/q/health/ready").build(), restResponse().add("status", "UP"));
    }

    private static RequestMatcher productQuery(String id) {
        return graphQlRequest()
            .query(Pattern.compile("query product\\(\\$id: String!\\) \\{ product\\(id: \\$id\\) \\{id name (description )?price} }"))
            .variable("id", id)
            .build();
    }

    @Override protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("received {}:{}", request.getMethod(), request.getRequestURI());

        if (request.getRequestURI().endsWith("wunderbar-stubbing/graphql")) {
            addExpectation(request, response);
        } else {
            matchExpectation(request, response);
        }
    }

    @SuppressWarnings("unused")
    private void addExpectation(HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("not yet implemented"); // TODO
    }

    @SneakyThrows(IOException.class)
    private void matchExpectation(HttpServletRequest request, HttpServletResponse response) {
        var body = request.getReader().lines().collect(joining());
        log.debug("---\n{}\n---", body);
        var match = EXPECTATIONS.entrySet().stream()
            .peek(entry -> log.debug("match {}", entry.getKey()))
            .filter(entry -> entry.getKey().matches(request, body))
            .peek(entry -> log.debug("matched!"))
            .findFirst().orElseGet(MockServlet::noMatch);
        match.getValue().apply(match.getKey(), response);
    }

    private static Entry<RequestMatcher, ResponseSupplier> noMatch() {
        var message = "no matching expectation found";
        log.debug(message);
        return Map.entry(restRequest().build(), restError().detail(message));
    }
}
