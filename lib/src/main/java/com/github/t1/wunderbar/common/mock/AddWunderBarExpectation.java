package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.common.mock.RequestMatcher.BodyMatcher;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.function.Function;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQL;
import static com.github.t1.wunderbar.common.mock.MockService.addExpectation;
import static com.github.t1.wunderbar.junit.http.HttpUtils.fromJson;

@Slf4j
@ToString
class AddWunderBarExpectation implements Function<HttpRequest, HttpResponse> {
    @Override public HttpResponse apply(HttpRequest request) {
        var variables = request.jsonBody().orElseThrow().asJsonObject().getJsonObject("variables");
        log.debug("add expectation: {}", variables);
        var matcherJson = variables.getJsonObject("matcher");
        var matcher = restMatcher(matcherJson);
        log.debug("    matcher: {}", matcher);
        var response = fromJson(variables.getJsonObject("response"), HttpResponse.class);
        log.debug("    response: {}", response);
        var expectation = addExpectation(matcher, response);
        return graphQL().with(builder -> expectationResponse(builder, expectation)).build();
    }

    private static void expectationResponse(JsonObjectBuilder builder, WunderBarMockExpectation expectation) {
        builder.add("data", Json.createObjectBuilder()
            .add("addWunderBarExpectation", Json.createObjectBuilder()
                .add("status", "ok")
                .add("id", expectation.getId())));
    }

    private static RequestMatcher restMatcher(JsonObject json) {
        return RequestMatcher.builder()
            .method(json.getString("method", null))
            .path(json.getString("path", null))
            .contentType(json.getString("contentType", null))
            .bodyMatcher(fromJson(json.get("bodyMatcher"), BodyMatcher.class))
            .build();
    }
}
