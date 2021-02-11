package com.github.t1.wunderbar.junit.consumer.integration;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@ToString @Getter @Builder
public class GraphQlResponseBody {
    Map<String, Object> data;
    List<GraphQlError> errors;
}
