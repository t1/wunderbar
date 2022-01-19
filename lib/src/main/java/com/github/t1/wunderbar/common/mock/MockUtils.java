package com.github.t1.wunderbar.common.mock;

import javax.json.Json;

import static com.github.t1.wunderbar.common.mock.GraphQLBodyMatcher.*;

public class MockUtils {
    public static RequestMatcher productQuery(String id) {
        return graphQlRequest()
            .queryPattern("query product\\(\\$id: String!\\) \\{ product\\(id: \\$id\\) \\{id name (description )?price} }")
            .variables(Json.createObjectBuilder().add("id", id).build())
            .build();
    }
}
