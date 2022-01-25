package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQLResponse;

@Slf4j
@ToString @EqualsAndHashCode(callSuper = true)
class CleanupWunderBarExpectation extends GraphQLMockExpectation {
    CleanupWunderBarExpectation() {super("mutation cleanupWunderBarExpectation { cleanupWunderBarExpectation }");}

    @Override public HttpResponse handle(HttpRequest request) {
        MockService.cleanup();
        return graphQLResponse().with(CleanupWunderBarExpectation::finalResponse).build();
    }

    private static void finalResponse(JsonObjectBuilder builder) {
        builder.add("data", Json.createObjectBuilder()
            .add("cleanupWunderBarExpectation", "ok"));
    }
}
