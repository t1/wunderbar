package com.github.t1.wunderbar.junit.http;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.With;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;

import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static com.github.t1.wunderbar.junit.http.HttpUtils.JSONB;
import static com.github.t1.wunderbar.junit.http.HttpUtils.optional;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

@Value @Builder @With
public class HttpServerRequest {
    public static HttpServerRequest from(Properties properties, Optional<String> body) {
        var builder = HttpServerRequest.builder();
        optional(properties, "Method").ifPresent(builder::method);
        optional(properties, "URI").map(URI::create).ifPresent(builder::uri);
        optional(properties, ACCEPT).map(MediaType::valueOf).ifPresent(builder::accept);
        optional(properties, CONTENT_TYPE).map(MediaType::valueOf).ifPresent(builder::contentType);
        body.ifPresent(builder::body);
        return builder.build();
    }

    @Default String method = "GET";
    @Default URI uri = URI.create("/");
    @Default MediaType contentType = APPLICATION_JSON_UTF8;
    @Default MediaType accept = APPLICATION_JSON_UTF8;
    @Default Optional<String> body = Optional.empty();

    @Override public String toString() { return (headerProperties() + "\n" + body.orElse("")).trim(); }

    public String headerProperties() {
        return "" +
            "Method: " + method + "\n" +
            "URI: " + uri + "\n" +
            ((accept == null) ? "" : ACCEPT + ": " + accept + "\n") +
            ((contentType == null) ? "" : CONTENT_TYPE + ": " + contentType + "\n");
    }

    @SuppressWarnings("unused")
    public static class HttpServerRequestBuilder {
        public HttpServerRequestBuilder body(Object body) {
            // JSON-B may produce a leading nl, but we want only a trailing nl
            return body(JSONB.toJson(body).trim() + "\n");
        }

        public HttpServerRequestBuilder body(String body) {
            body$value = Optional.of(body);
            body$set = true;
            return this;
        }
    }
}
