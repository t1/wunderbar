package com.github.t1.wunderbar.mock;

import static com.github.t1.wunderbar.mock.GraphQLBodyMatcher.graphQlRequest;

public class MockUtils {
    public static RequestMatcher productQuery(String id) {
        return graphQlRequest()
            .query("query product\\(\\$id: String!\\) \\{ product\\(id: \\$id\\) \\{id name (description )?price} }")
            .variable("id", id)
            .build();
    }
}
