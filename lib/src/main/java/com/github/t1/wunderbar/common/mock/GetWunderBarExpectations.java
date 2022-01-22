package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpUtils;
import lombok.ToString;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQLResponse;

@ToString
class GetWunderBarExpectations extends GraphQLMockExpectation {
    GetWunderBarExpectations() {
        super("query getWunderBarExpectations { getWunderBarExpectations() {id} }");
    }

    @Override public HttpResponse handle(HttpRequest request) {
        return graphQLResponse().with(this::getExpectations).build();
    }

    private void getExpectations(JsonObjectBuilder builder) {
        var expectations = Json.createArrayBuilder();
        MockService.getExpectations().stream()
            .map(HttpUtils::readJson)
            .forEach(expectations::add);
        builder.add("data", Json.createObjectBuilder()
            .add("getWunderBarExpectations", expectations));
    }
}
