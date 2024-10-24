package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQLResponse;

@Slf4j
@ToString @EqualsAndHashCode(callSuper = true)
class AddWunderBarExpectation extends GraphQLMockExpectation {
    AddWunderBarExpectation() {
        super("mutation addWunderBarExpectation" +
              "($request: HttpRequestInput!, $depletion: DepletionInput!, $response: HttpResponseInput!) { " +
              "addWunderBarExpectation(request: $request, depletion: $depletion, response: $response) {id status} }");
    }

    @Override public HttpResponse handle(HttpRequest request) {
        var variables = request.as(Body.class).variables();
        var expectation = addExpectation(variables);
        return graphQLResponse().with(builder -> expectationResponse(builder, expectation)).build();
    }

    private WunderBarMockExpectation addExpectation(Variables variables) {
        log.debug("add expectation: {}", variables);
        return MockService.addExpectation(variables.request, variables.depletion.maxCallCount, variables.response);
    }

    private static void expectationResponse(JsonObjectBuilder builder, WunderBarMockExpectation expectation) {
        builder.add("data", Json.createObjectBuilder()
                .add("addWunderBarExpectation", Json.createObjectBuilder()
                        .add("status", "ok")
                        .add("id", expectation.getId())));
    }

    public record Body(
            String query,
            Variables variables,
            String operationName) {}

    public record Variables(
            HttpRequest request,
            Depletion depletion,
            HttpResponse response) {}

    public record Depletion(int maxCallCount) {}
}
