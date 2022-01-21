package com.github.t1.wunderbar.junit.http;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static com.github.t1.wunderbar.junit.http.HttpUtils.JSONB;
import static com.github.t1.wunderbar.junit.http.HttpUtils.fromJson;
import static com.github.t1.wunderbar.junit.http.HttpUtils.optional;
import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;
import static javax.ws.rs.core.Response.Status.OK;

@Value @Builder @With
public class HttpResponse {
    public static HttpResponse from(Properties properties, Optional<String> body) {
        var builder = HttpResponse.builder();
        optional(properties, "Status").map(HttpUtils::toStatus).ifPresent(builder::status);
        optional(properties, "Content-Type").map(MediaType::valueOf).ifPresent(builder::contentType);
        body.ifPresent(builder::body);
        return builder.build();
    }

    @JsonbTypeAdapter(StatusTypeAdapter.class)
    StatusType status;
    @JsonbTypeAdapter(MediaTypeAdapter.class)
    MediaType contentType;
    String body;

    @JsonbCreator public HttpResponse(StatusType status, MediaType contentType, String body) {
        this.status = (status == null) ? OK : status;
        this.contentType = (contentType == null) ? APPLICATION_JSON_UTF8 : contentType;
        this.body = body;
    }

    @Override public String toString() {return (headerProperties() + (body == null ? "" : body)).trim();}

    public String headerProperties() {
        return "" +
               "Status: " + getStatusString() + "\n" +
               "Content-Type: " + contentType + "\n";
    }

    public String getStatusString() {return status.getStatusCode() + " " + status.getReasonPhrase();}

    public Optional<String> body() {return Optional.ofNullable(body);}

    public Optional<JsonValue> getJsonBody() {return body().map(HttpUtils::toJson);}

    @SuppressWarnings("unused")
    public static class HttpResponseBuilder {
        public HttpResponseBuilder with(String field, Object value) {
            return with(builder -> builder.add(field, readJson(value)));
        }

        public HttpResponseBuilder with(Consumer<JsonObjectBuilder> consumer) {
            var builder = (body == null) ? Json.createObjectBuilder() : fromJson(body);
            consumer.accept(builder);
            this.body = builder.build().toString();
            return this;
        }

        public HttpResponseBuilder body(Object body) {
            if (body == JsonValue.NULL) return body(null);
            // JSON-B may produce a leading nl, but we want only a trailing nl
            return body(JSONB.toJson(body).trim() + "\n");
        }

        public HttpResponseBuilder body(String body) {
            this.body = body;
            return this;
        }

        public HttpResponseBuilder status(int status) {
            return status(Status.fromStatusCode(status));
        }

        public HttpResponseBuilder status(StatusType status) {
            this.status = status;
            return this;
        }
    }
}
