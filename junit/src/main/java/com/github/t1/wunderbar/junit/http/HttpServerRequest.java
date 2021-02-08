package com.github.t1.wunderbar.junit.http;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Value @Builder
public class HttpServerRequest {
    public static final MediaType APPLICATION_JSON_UTF8 = APPLICATION_JSON_TYPE.withCharset("utf-8");
    static final Jsonb JSONB = JsonbBuilder.create(new JsonbConfig().withFormatting(true));

    @Default String method = "GET";
    @Default URI uri = URI.create("/");
    @Default MediaType contentType = APPLICATION_JSON_UTF8;
    @Default MediaType accept = APPLICATION_JSON_UTF8;
    @Default Optional<String> body = Optional.empty();

    @Override public String toString() { return (headerProperties() + body.orElse("")).trim(); }

    public String headerProperties() {
        return "" +
            "Method: " + method + "\n" +
            "URI: " + uri + "\n" +
            "Accept: " + accept + "\n" +
            "Content-Type: " + contentType + "\n";
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
