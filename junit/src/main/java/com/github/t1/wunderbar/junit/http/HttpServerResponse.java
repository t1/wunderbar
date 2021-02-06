package com.github.t1.wunderbar.junit.http;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
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

    @Override public String toString() { return (headerProperties() + body.orElse("")).trim(); }

    public String headerProperties() {
        return "" +
            "Status: " + status.getStatusCode() + " " + status.getReasonPhrase() + "\n" +
            ((contentType == null) ? "" : "Content-Type: " + contentType + "\n");
    }

    @SuppressWarnings("unused")
    public static class HttpServerResponseBuilder {
        public HttpServerResponseBuilder body(Object body) {
            // JSON-B may produce a leading nl, but we want only a trailing nl
            return body(JSONB.toJson(body).trim() + "\n");
        }

        public HttpServerResponseBuilder body(String body) {
            body$value = Optional.of(body);
            body$set = true;
            return this;
        }
    }

    private static final Jsonb JSONB = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
}
