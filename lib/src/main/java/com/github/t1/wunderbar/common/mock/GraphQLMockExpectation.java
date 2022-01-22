package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import lombok.RequiredArgsConstructor;

import javax.json.JsonObject;

@RequiredArgsConstructor
abstract class GraphQLMockExpectation extends WunderBarMockExpectation {
    private final String query;

    @Override public boolean matches(HttpRequest request) {
        return matchesGraphQl(request);
    }

    private boolean matchesGraphQl(HttpRequest request) {
        return request.getUri().getPath().endsWith("/graphql")
               && request.isJson()
               && matchesQuery(request.jsonValue().asJsonObject());
    }

    private boolean matchesQuery(JsonObject body) {
        return body.containsKey("query")
               && body.getString("query").equals(query);
    }
}
