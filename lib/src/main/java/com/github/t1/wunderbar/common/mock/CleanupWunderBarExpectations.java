package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQLResponse;

@Slf4j
@ToString @EqualsAndHashCode(callSuper = true)
class CleanupWunderBarExpectations extends GraphQLMockExpectation {
    CleanupWunderBarExpectations() {super("mutation cleanupWunderBarExpectations { cleanupWunderBarExpectations }");}

    @Override public HttpResponse handle(HttpRequest request) {
        MockService.cleanup();
        return graphQLResponse().with(this::finalResponse).build();
    }

    private void finalResponse(JsonObjectBuilder builder) {
        builder.add("data", Json.createObjectBuilder()
            .add("cleanupWunderBarExpectations", "ok"));
    }
}
