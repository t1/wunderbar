package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.ProblemDetails;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.common.Utils.prefix;
import static java.util.Collections.unmodifiableList;

@Slf4j
public class MockService {
    private static final List<WunderBarMockExpectation> EXPECTATIONS = new ArrayList<>();
    private static final int INITIAL_SIZE;

    static {
        addExpectation(new AddWunderBarExpectation());
        addExpectation(new GetWunderBarExpectations());
        addExpectation(new CleanupWunderBarExpectation());
        INITIAL_SIZE = EXPECTATIONS.size();
    }

    public static WunderBarMockExpectation addExpectation(HttpRequest expectedRequest, HttpResponse response) {
        return addExpectation(new WunderBarMockInteractionExpectation(expectedRequest, response));
    }

    private static WunderBarMockExpectation addExpectation(WunderBarMockExpectation expectation) {
        EXPECTATIONS.add(expectation);
        return expectation;
    }

    public static List<WunderBarMockExpectation> getExpectations() {
        return unmodifiableList(EXPECTATIONS.subList(INITIAL_SIZE, EXPECTATIONS.size()));
    }

    public static void cleanup() {
        while (EXPECTATIONS.size() > INITIAL_SIZE) {
            var expectation = EXPECTATIONS.get(INITIAL_SIZE);
            log.debug("remove unused expectation: {}", expectation);
            EXPECTATIONS.remove(expectation);
        }
    }


    public HttpResponse service(HttpRequest rawRequest) {
        var request = rawRequest.withFormattedBody();
        log.info("received request:\n{}", prefix("< ", request.toString()));
        WunderBarMockExpectation expectation = findExpectationMatching(request);
        if (expectation == null) {
            var message = "no matching expectation found";
            log.debug(message);
            return ProblemDetails.builder().detail(message).build().toResponse();
        }
        if (expectation.getId() >= INITIAL_SIZE) EXPECTATIONS.remove(expectation);
        return expectation.handle(request);
    }

    private WunderBarMockExpectation findExpectationMatching(HttpRequest request) {
        return EXPECTATIONS.stream()
            .peek(expectation -> log.debug("{} (of {}) match {}", expectation.getId(), EXPECTATIONS.size(), expectation))
            .filter(expectation -> expectation.matches(request))
            .peek(expectation -> log.debug("matched! => {}", expectation))
            .findFirst().orElse(null);
    }
}
