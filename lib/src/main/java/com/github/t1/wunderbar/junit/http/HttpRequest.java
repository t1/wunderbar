package com.github.t1.wunderbar.junit.http;

import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.http.Authorization.Dummy;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.With;

import javax.json.JsonValue;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;

import static com.github.t1.wunderbar.common.Utils.isCompatible;
import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static com.github.t1.wunderbar.junit.http.HttpUtils.JSONB;
import static com.github.t1.wunderbar.junit.http.HttpUtils.firstMediaType;
import static com.github.t1.wunderbar.junit.http.HttpUtils.optional;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Value @Builder @With
public class HttpRequest {
    public static HttpRequest from(Properties properties, Optional<String> body) {
        var builder = HttpRequest.builder();
        optional(properties, "Method").ifPresent(builder::method);
        optional(properties, "URI").map(URI::create).ifPresent(builder::uri);
        optional(properties, ACCEPT).map(MediaType::valueOf).ifPresent(builder::accept);
        optional(properties, CONTENT_TYPE).ifPresent(builder::contentType);
        optional(properties, AUTHORIZATION).map(Authorization::valueOf).ifPresent(value -> {
            assert value instanceof Dummy : "expected " + AUTHORIZATION + " header to be the dummy value!";
            builder.authorization(value);
        });
        body.ifPresent(builder::body);
        return builder.build();
    }

    @Default String method = "GET";
    @Default URI uri = URI.create("/");
    Authorization authorization;
    @Default MediaType contentType = APPLICATION_JSON_UTF8;
    @Default MediaType accept = APPLICATION_JSON_UTF8;
    @Default Optional<String> body = Optional.empty();

    public HttpRequest withFormattedBody() {return (isJson()) ? withBody(body.map(Utils::formatJson)) : this;}

    @Override public String toString() {return (headerProperties() + "\n" + body.orElse("")).trim();}

    public String headerProperties() {
        return "" +
               "Method: " + method + "\n" +
               "URI: " + uri + "\n" +
               ((accept == null) ? "" : ACCEPT + ": " + accept + "\n") +
               ((contentType == null) ? "" : CONTENT_TYPE + ": " + contentType + "\n") +
               ((authorization == null) ? "" : AUTHORIZATION + ": " + authorization + "\n");
    }

    public boolean hasBody() {return body.isPresent();}

    public boolean isJson() {return hasBody() && isCompatible(APPLICATION_JSON_TYPE, contentType);}

    public Optional<JsonValue> jsonBody() {return body.map(Utils::readJson);}

    @SuppressWarnings("unused")
    public static class HttpRequestBuilder {
        public HttpRequestBuilder contentType(String contentType) {
            return contentType(firstMediaType(contentType));
        }

        public HttpRequestBuilder contentType(MediaType contentType) {
            this.contentType$set = true;
            this.contentType$value = contentType;
            return this;
        }

        public HttpRequestBuilder body(Object body) {
            // JSON-B may produce a leading nl, but we want only a trailing nl
            return body(JSONB.toJson(body).trim() + "\n");
        }

        public HttpRequestBuilder body(String body) {
            body$value = Optional.of(body);
            body$set = true;
            return this;
        }
    }
}
