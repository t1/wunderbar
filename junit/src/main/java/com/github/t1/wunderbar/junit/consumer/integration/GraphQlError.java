package com.github.t1.wunderbar.junit.consumer.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@ToString @Getter @Setter @NoArgsConstructor
@Builder @AllArgsConstructor(access = PRIVATE)
public class GraphQlError {
    String message;
    @Singular Map<String, Object> extensions;
}
