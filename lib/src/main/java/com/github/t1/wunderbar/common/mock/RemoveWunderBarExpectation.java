package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.ToString;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQL;

@ToString
class RemoveWunderBarExpectation extends RestResponseSupplier {
    @Override public HttpResponse apply(HttpRequest request) {
        var variables = request.jsonBody().orElseThrow().asJsonObject().getJsonObject("variables");
        MockService.removeExpectation(variables.getInt("id"));
        return graphQL().with(RemoveWunderBarExpectation::removeResponse).build().apply(request);
    }

    private static void removeResponse(JsonObjectBuilder builder) {
        builder.add("data", Json.createObjectBuilder()
            .add("removeWunderBarExpectation", "ok"));
    }
}
