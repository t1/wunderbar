package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import jakarta.json.JsonObject;

@RequiredArgsConstructor @EqualsAndHashCode(callSuper = true)
abstract class GraphQLMockExpectation extends WunderBarMockExpectation {
    private final String expectedQuery;

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
               && body.getString("query").equals(expectedQuery);
    }
}
