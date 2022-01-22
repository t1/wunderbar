package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.ToString;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQLResponse;

@ToString
class RemoveWunderBarExpectation extends GraphQLMockExpectation {
    RemoveWunderBarExpectation() {
        super("mutation removeWunderBarExpectation($id: Int!) { removeWunderBarExpectation(id: $id) }");
    }

    @Override public HttpResponse handle(HttpRequest request) {
        var variables = request.get("variables").asJsonObject();
        MockService.removeExpectation(variables.getInt("id"));
        return graphQLResponse().with(RemoveWunderBarExpectation::removeResponse).build();
    }

    private static void removeResponse(JsonObjectBuilder builder) {
        builder.add("data", Json.createObjectBuilder()
            .add("removeWunderBarExpectation", "ok"));
    }
}
