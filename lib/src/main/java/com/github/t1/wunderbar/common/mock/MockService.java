package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.ProblemDetails;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Slf4j
public class MockService {
    private static final List<WunderBarMockExpectation> EXPECTATIONS = new ArrayList<>();

    static {
        addExpectation(new AddWunderBarExpectation());
        addExpectation(new GetWunderBarExpectations());
        addExpectation(new RemoveWunderBarExpectation());
        addExpectation(HttpRequest.builder().uri("/q/health/ready").build(),
            HttpResponse.builder().with("status", "UP").build());
    }

    public static WunderBarMockExpectation addExpectation(HttpRequest expectedRequest, HttpResponse response) {
        var expectation = new WunderBarMockExpectation() {
            @Override public String toString() {
                return expectedRequest.getMethod() + " " + expectedRequest.getUri() + " => " + response.getStatusString();
            }

            @Override public boolean matches(HttpRequest request) {return expectedRequest.matches(request);}

            @Override public HttpResponse handle(HttpRequest request) {return response;}
        };
        return addExpectation(expectation);
    }

    private static WunderBarMockExpectation addExpectation(WunderBarMockExpectation expectation) {
        EXPECTATIONS.add(expectation);
        return expectation;
    }

    public static List<WunderBarMockExpectation> getExpectations() {
        return unmodifiableList(EXPECTATIONS);
    }

    public static void removeExpectation(int id) {
        log.debug("remove expectation: {}", id);
        var expectation = EXPECTATIONS.stream().filter(e -> e.getId() == id)
            .findFirst().orElseThrow(() -> new IllegalStateException("can't remove expectation " + id + ": not found."));
        EXPECTATIONS.remove(expectation);
    }

    public HttpResponse service(HttpRequest rawRequest) {
        var request = rawRequest.withFormattedBody();
        log.info("received request:\n{}", request);
        WunderBarMockExpectation match = EXPECTATIONS.stream()
            .peek(expectation -> log.debug("{} match {}", expectation.getId(), expectation))
            .filter(expectation -> expectation.matches(request))
            .peek(expectation -> log.debug("matched! => {}", expectation))
            .findFirst().orElse(FINAL_MATCH);
        return match.handle(request);
    }

    private static final WunderBarMockExpectation FINAL_MATCH = new WunderBarMockExpectation() {
        private static final String message = "no matching expectation found";

        @Override public String toString() {return message;}

        @Override public boolean matches(HttpRequest request) {return true;}

        @Override public HttpResponse handle(HttpRequest req) {
            log.debug(message);
            return ProblemDetails.builder().detail(message).build().toResponse();
        }
    };
}
