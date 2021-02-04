package com.github.t1.wunderbar.junit.http;

import lombok.Builder;
import lombok.Value;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Optional;

@Value @Builder
public class HttpServerRequest {
    String method;
    URI uri;
    MediaType contentType;
    MediaType accept;
    Optional<String> body;

    @Override public String toString() {
        return (method + " " + uri + "\n" +
            ((accept == null) ? "" : "Accept: " + accept + "\n") +
            ((contentType == null) ? "" : "Content-Type: " + contentType + "\n") +
            body.orElse("")
        ).trim();
    }
}
