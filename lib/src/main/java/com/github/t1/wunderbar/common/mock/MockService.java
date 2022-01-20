package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.common.Utils.formatJson;
import static com.github.t1.wunderbar.common.mock.GraphQLBodyMatcher.graphQlRequest;
import static com.github.t1.wunderbar.common.mock.RestErrorSupplier.restError;
import static com.github.t1.wunderbar.common.mock.RestResponseSupplier.restResponse;
import static com.github.t1.wunderbar.common.mock.WunderBarMockExpectation.exp;

@Slf4j
public class MockService {
    static final List<WunderBarMockExpectation> EXPECTATIONS = new ArrayList<>();

    static {
        addExpectation(graphQlRequest().query(
                    "mutation addWunderBarExpectation($matcher: RequestMatcherInput!, $responseSupplier: ResponseSupplierInput!) " +
                    "{ addWunderBarExpectation(matcher: $matcher, responseSupplier: $responseSupplier) {id status} }")
                .build(),
            new AddWunderBarExpectation());
        addExpectation(graphQlRequest().query(
                    "mutation removeWunderBarExpectation($id: Int!) { removeWunderBarExpectation(id: $id) }")
                .build(),
            new RemoveWunderBarExpectation());
        addExpectation(RequestMatcher.builder().path("/q/health/ready").build(), restResponse().add("status", "UP"));
    }

    public static WunderBarMockExpectation addExpectation(RequestMatcher queryMatcher, ResponseSupplier responseSupplier) {
        var expectation = exp(queryMatcher, responseSupplier);
        EXPECTATIONS.add(expectation);
        return expectation;
    }

    public static void removeExpectation(int id) {
        var expectation = EXPECTATIONS.stream().filter(e -> e.getId() == id)
            .findFirst().orElseThrow(() -> new IllegalStateException("can't remove expectation " + id + ": not found."));
        EXPECTATIONS.remove(expectation);
    }

    public HttpResponse service(HttpRequest request) throws IOException {
        log.info("received {}:{}", request.getMethod(), request.getUri());
        var body = request.jsonBody().orElse(null);
        log.debug("received body:\n{}", formatJson(body));
        WunderBarMockExpectation match = MockService.noMatch();
        for (int i = 0, expectationsSize = EXPECTATIONS.size(); i < expectationsSize; i++) {
            WunderBarMockExpectation expectation = EXPECTATIONS.get(i);
            log.debug("{} match {}", i, expectation.getRequestMatcher());
            if (expectation.getRequestMatcher().matches(request, body)) {
                log.debug("matched! => {}", expectation.getResponseSupplier());
                match = expectation;
                break;
            }
        }
        return match.getResponseSupplier().apply(request);
    }

    private static WunderBarMockExpectation noMatch() {
        var message = "no matching expectation found";
        return exp(RequestMatcher.builder().build(), new ResponseSupplier() {
            @Override public String toString() {return message;}

            @Override public HttpResponse apply(HttpRequest req) {
                log.debug(message);
                return restError().detail(message).build().apply(req);
            }
        });
    }
}
