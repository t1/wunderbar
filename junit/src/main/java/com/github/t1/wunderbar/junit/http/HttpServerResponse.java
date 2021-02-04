package com.github.t1.wunderbar.junit.http;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.StatusType;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.OK;

@Value @Builder
public class HttpServerResponse {
    @Default StatusType status = OK;
    @Default MediaType contentType = APPLICATION_JSON_TYPE.withCharset("utf-8");
    @Default Optional<String> body = Optional.empty();

    @Override public String toString() {
        return ("" +
            "Status: " + status + "\n" +
            "Content-Type: " + contentType + "\n" +
            body.orElse("")
        ).trim();
    }
}
