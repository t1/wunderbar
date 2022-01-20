package com.github.t1.wunderbar.common.mock;

import lombok.Builder;
import lombok.Value;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.function.Predicate;

@Value @Builder(buildMethodName = "internalBuild")
public class GraphQLBodyMatcher implements Predicate<JsonValue> {
    public static GraphQLBodyMatcher.GraphQLBodyMatcherBuilder graphQlRequest() {return GraphQLBodyMatcher.builder();}

    String query;
    String queryPattern;
    JsonObject variables;
    String operationName;

    public static class GraphQLBodyMatcherBuilder {
        public RequestMatcher build() {
            var bodyMatcher = internalBuild();
            return RequestMatcher.builder()
                .method("POST")
                .path(".*/graphql")
                .contentType("application/.*json.*")
                .bodyMatcher(bodyMatcher)
                .build();
        }
    }

    @Override public boolean test(JsonValue jsonValue) {return matchBody(jsonValue);}

    private boolean matchBody(JsonValue jsonValue) {
        switch (jsonValue.getValueType()) {
            case NULL:
                return queryPattern == null && query == null;
            case OBJECT:
                return matchBody(jsonValue.asJsonObject());
            default:
                throw new IllegalArgumentException("unexpected json body type: " + jsonValue.getValueType());
        }
    }

    private boolean matchBody(JsonObject jsonObject) {
        return matchQuery(jsonObject) && matchVariables(jsonObject) && matchOperationName(jsonObject);
    }

    private boolean matchQuery(JsonObject jsonObject) {
        var actual = jsonObject.getString("query");
        if (query != null) return actual.equals(query);
        if (queryPattern != null) return actual.matches(queryPattern);
        return actual == null || actual.isBlank();
    }

    private boolean matchVariables(JsonObject jsonObject) {
        if (variables == null) return true;
        var actualVariables = jsonObject.getJsonObject("variables");
        return this.variables.equals(actualVariables);
    }

    private boolean matchOperationName(JsonObject jsonObject) {
        if (GraphQLBodyMatcher.this.operationName == null) return true;
        var actualOperationName = jsonObject.getString("operationName");
        return this.operationName.equals(actualOperationName);
    }
}
