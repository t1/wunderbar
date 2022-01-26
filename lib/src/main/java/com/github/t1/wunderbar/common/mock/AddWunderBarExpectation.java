package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static com.github.t1.wunderbar.common.Utils.prefix;
import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQLResponse;
import static com.github.t1.wunderbar.junit.http.HttpUtils.fromJson;
import static com.github.t1.wunderbar.junit.http.HttpUtils.toFlatString;

@Slf4j
@ToString @EqualsAndHashCode(callSuper = true)
class AddWunderBarExpectation extends GraphQLMockExpectation {
    AddWunderBarExpectation() {
        super("mutation addWunderBarExpectation" +
              "($request: HttpRequestInput!, $depletion: DepletionInput!, $response: HttpResponseInput!) { " +
              "addWunderBarExpectation(request: $request, depletion: $depletion, response: $response) {id status} }");
    }

    @Override public HttpResponse handle(HttpRequest request) {
        var variables = request.get("variables").asJsonObject();
        var expectation = addExpectation(variables);
        return graphQLResponse().with(builder -> expectationResponse(builder, expectation)).build();
    }

    private WunderBarMockExpectation addExpectation(JsonObject variables) {
        log.debug("add expectation: {}", variables);

        var request = fromJson(variables.getJsonObject("request"), HttpRequest.class);
        log.debug("request:\n{}", prefix("    ", request.toString()));

        var depletion = variables.getJsonObject("depletion");
        log.debug("depletion: {}", toFlatString(depletion));
        var maxCallCount = depletion.getInt("maxCallCount");

        var response = fromJson(variables.getJsonObject("response"), HttpResponse.class);
        log.debug("response:\n{}", prefix("    ", response.toString()));

        return MockService.addExpectation(request, maxCallCount, response);
    }

    private static void expectationResponse(JsonObjectBuilder builder, WunderBarMockExpectation expectation) {
        builder.add("data", Json.createObjectBuilder()
            .add("addWunderBarExpectation", Json.createObjectBuilder()
                .add("status", "ok")
                .add("id", expectation.getId())));
    }
}
