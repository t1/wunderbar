package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import lombok.Builder;
import lombok.Value;

import javax.json.JsonObject;

@Value @Builder(buildMethodName = "internalBuild")
public class GraphQLBodyMatcher {
    public static GraphQLBodyMatcher.GraphQLBodyMatcherBuilder graphQlRequest() {return GraphQLBodyMatcher.builder();}

    String query;
    JsonObject variables;
    String operationName;

    public static class GraphQLBodyMatcherBuilder {
        public HttpRequest build() {
            var body = internalBuild();
            return HttpRequest.builder()
                .method("POST")
                .uri("/graphql")
                .contentType("application/json")
                .body(body)
                .build();
        }
    }
}
