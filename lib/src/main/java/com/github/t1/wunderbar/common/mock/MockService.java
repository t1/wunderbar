package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.ProblemDetails;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.github.t1.wunderbar.common.mock.GraphQLBodyMatcher.graphQlRequest;
import static com.github.t1.wunderbar.junit.http.HttpUtils.formatJson;

@Slf4j
public class MockService {
    private static final List<WunderBarMockExpectation> EXPECTATIONS = new ArrayList<>();

    static {
        addExpectation(graphQlRequest().query(
                    "mutation addWunderBarExpectation($matcher: RequestMatcherInput!, $response: HttpResponseInput!) " +
                    "{ addWunderBarExpectation(matcher: $matcher, response: $response) {id status} }")
                .build(),
            new AddWunderBarExpectation());
        addExpectation(graphQlRequest().query(
                    "mutation removeWunderBarExpectation($id: Int!) { removeWunderBarExpectation(id: $id) }")
                .build(),
            new RemoveWunderBarExpectation());
        addExpectation(RequestMatcher.builder().path("/q/health/ready").build(),
            HttpResponse.builder().with("status", "UP").build());
    }

    public static WunderBarMockExpectation addExpectation(RequestMatcher queryMatcher, HttpResponse response) {
        return addExpectation(queryMatcher, new Function<>() {
            @Override public String toString() {return "return: " + response;}

            @Override public HttpResponse apply(HttpRequest request) {return response;}
        });
    }

    private static WunderBarMockExpectation addExpectation(RequestMatcher queryMatcher, Function<HttpRequest, HttpResponse> handler) {
        var expectation = WunderBarMockExpectation.of(queryMatcher, handler);
        EXPECTATIONS.add(expectation);
        return expectation;
    }

    public static void removeExpectation(int id) {
        log.debug("remove expectation: {}", id);
        var expectation = EXPECTATIONS.stream().filter(e -> e.getId() == id)
            .findFirst().orElseThrow(() -> new IllegalStateException("can't remove expectation " + id + ": not found."));
        EXPECTATIONS.remove(expectation);
    }

    public HttpResponse service(HttpRequest request) throws IOException {
        log.info("received {} {} {}", request.getMethod(), request.getUri(), request.getContentType());
        var body = request.jsonBody().orElse(null);
        if (body == null) log.debug("received no body");
        else log.debug("received body:\n{}", formatJson(body));
        WunderBarMockExpectation match = NO_MATCH;
        for (int i = 0, expectationsSize = EXPECTATIONS.size(); i < expectationsSize; i++) {
            WunderBarMockExpectation expectation = EXPECTATIONS.get(i);
            log.debug("{} match {}", i, expectation.getRequestMatcher());
            if (expectation.getRequestMatcher().matches(request, body)) {
                log.debug("matched! => {}", expectation.getHandler());
                match = expectation;
                break;
            }
        }
        return match.getHandler().apply(request);
    }

    private static final WunderBarMockExpectation NO_MATCH = WunderBarMockExpectation.of(
        RequestMatcher.builder().build(),
        new Function<>() {
            private static final String message = "no matching expectation found";

            @Override public String toString() {return message;}

            @Override public HttpResponse apply(HttpRequest req) {
                log.debug(message);
                return ProblemDetails.builder().detail(message).build().toResponse();
            }
        });
}
