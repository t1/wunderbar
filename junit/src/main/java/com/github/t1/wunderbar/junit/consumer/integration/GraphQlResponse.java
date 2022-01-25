package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.common.Internal;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@ToString @Getter @Builder
public @Internal class GraphQlResponse {
    Map<String, Object> data;
    List<GraphQLErrorResponse> errors;
}
