package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.ProblemDetails;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.common.Utils.prefix;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static java.util.Collections.unmodifiableList;

@Slf4j
public class MockService {
    private static final List<WunderBarMockExpectation> EXPECTATIONS = new ArrayList<>();
    private static final int INITIAL_SIZE;

    static {
        addExpectation(new AddWunderBarExpectation());
        addExpectation(new GetWunderBarExpectations());
        addExpectation(new CleanupWunderBarExpectations());
        INITIAL_SIZE = EXPECTATIONS.size();
    }

    public static WunderBarMockExpectation addExpectation(HttpRequest expectedRequest, int maxCallCount, HttpResponse response) {
        return addExpectation(WunderBarMockInteractionExpectation.builder()
                .expectedRequest(expectedRequest)
                .maxCallCount(maxCallCount)
                .response(response)
                .build());
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
        var request = rawRequest.normalized();
        log.info("received request:\n{}", prefix("< ", request.toString()));
        WunderBarMockExpectation expectation = findExpectationMatching(request);
        if (expectation == null) {
            var message = "no matching expectation found";
            log.debug(message);
            return ProblemDetails.builder().status(BAD_REQUEST).detail(message).build().toResponse();
        }
        var response = expectation.handle(request);
        if (!expectation.moreInvocationsAllowed()) {
            log.debug("expectation is depleted... removing {}", expectation);
            EXPECTATIONS.remove(expectation);
        }
        return response;
    }

    private WunderBarMockExpectation findExpectationMatching(HttpRequest request) {
        return EXPECTATIONS.stream()
                .peek(expectation -> log.debug("{} (of {}) match {}", expectation.getId(), EXPECTATIONS.size(), expectation))
                .filter(expectation -> expectation.matches(request))
                .findFirst().orElse(null);
    }
}
