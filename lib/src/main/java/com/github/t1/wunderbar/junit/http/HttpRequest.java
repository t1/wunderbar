package com.github.t1.wunderbar.junit.http;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.http.Authorization.Dummy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.With;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbCreator;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.t1.wunderbar.common.Utils.jsonNonAddDiff;
import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static com.github.t1.wunderbar.junit.http.HttpUtils.JSONB;
import static com.github.t1.wunderbar.junit.http.HttpUtils.firstMediaType;
import static com.github.t1.wunderbar.junit.http.HttpUtils.isCompatible;
import static com.github.t1.wunderbar.junit.http.HttpUtils.optional;
import static javax.json.JsonValue.ValueType.OBJECT;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static lombok.AccessLevel.NONE;

@Value @Builder @With @EqualsAndHashCode(exclude = "jsonValue")
public class HttpRequest {
    public static HttpRequest from(Properties properties, Optional<String> body) {
        var builder = HttpRequest.builder();
        optional(properties, "Method").ifPresent(builder::method);
        optional(properties, "URI").ifPresent(builder::uri);
        optional(properties, ACCEPT).map(MediaType::valueOf).ifPresent(builder::accept);
        optional(properties, CONTENT_TYPE).ifPresent(builder::contentType);
        optional(properties, AUTHORIZATION).map(Authorization::valueOf).ifPresent(value -> {
            assert value instanceof Dummy : "expected " + AUTHORIZATION + " header to be the dummy value!";
            builder.authorization(value);
        });
        body.ifPresent(builder::body);
        return builder.build();
    }

    String method;
    String uri;
    Authorization authorization;
    MediaType contentType;
    MediaType accept;
    String body;
    /** internal, lazily converted json */
    @Getter(NONE) AtomicReference<Optional<JsonValue>> jsonValue = new AtomicReference<>();

    @Internal @JsonbCreator public HttpRequest(
        String method,
        String uri,
        Authorization authorization,
        MediaType contentType,
        MediaType accept,
        String body
    ) {
        this.method = (method == null) ? "GET" : method;
        this.uri = (uri == null) ? "/" : uri;
        this.contentType = (contentType == null) ? APPLICATION_JSON_UTF8 : contentType;
        this.accept = (accept == null) ? APPLICATION_JSON_UTF8 : accept;
        this.authorization = authorization;
        this.body = body;
    }

    @Override public String toString() {return (headerProperties() + "\n" + body().orElse("")).trim();}

    public String headerProperties() {
        return "" +
               "Method: " + method + "\n" +
               "URI: " + uri + "\n" +
               ((accept == null) ? "" : ACCEPT + ": " + accept + "\n") +
               ((contentType == null) ? "" : CONTENT_TYPE + ": " + contentType + "\n") +
               ((authorization == null) ? "" : AUTHORIZATION + ": " + authorization + "\n");
    }

    public HttpRequest withFormattedBody() {return (isJson()) ? withBody(body().map(HttpUtils::formatJson).orElseThrow()) : this;}

    /**
     * Almost the same as <code>equals</code>, but
     * the content types only have to be compatible, and
     * <code>that</code> body may contain more fields than <code>this</code> does.
     */
    public boolean matches(HttpRequest that) {
        return this.method.equals(that.method)
               && this.uri.equals(that.uri)
               && (this.authorization == null || this.authorization.equals(that.authorization))
               && (this.contentType == null || this.contentType.isCompatible(that.contentType))
               && (this.accept == null || this.accept.isCompatible(that.accept))
               && (this.body == null || matchesBody(that));
    }

    private boolean matchesBody(HttpRequest that) {
        if (!this.isJson() || !that.isJson()) return false;
        return jsonNonAddDiff(this.jsonValue(), that.jsonValue()).findAny().isEmpty();
    }

    public URI getUri() {return URI.create(uri);}

    public String uri() {return uri;}

    public boolean hasBody() {return body != null;}

    public Optional<String> body() {return Optional.ofNullable(body);}

    public Optional<JsonValue> getJsonBody() {return jsonValue.updateAndGet(this::createOrGetJsonValue);}

    public boolean isJson() {return hasBody() && isCompatible(APPLICATION_JSON_TYPE, contentType);}

    public boolean isJsonObject() {return isJson() && jsonValue().getValueType() == OBJECT;}

    private Optional<JsonValue> createOrGetJsonValue(Optional<JsonValue> old) {
        //noinspection OptionalAssignedToNull // not initialized
        return (old == null) ? body().map(HttpUtils::readJson) : old;
    }

    public JsonValue jsonValue() {return getJsonBody().orElseThrow();}

    public JsonObject jsonObject() {return jsonValue().asJsonObject();}

    public boolean has(String field) {return isJsonObject() && jsonObject().containsKey(field);}

    public JsonValue get(String pointer) {return jsonObject().getValue("/" + pointer);}

    @SuppressWarnings("unused")
    public static class HttpRequestBuilder {
        public HttpRequestBuilder uri(URI uri) {
            return (uri == null) ? null : uri(uri.toString());
        }

        public HttpRequestBuilder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public HttpRequestBuilder contentType(String contentType) {
            return contentType(firstMediaType(contentType));
        }

        public HttpRequestBuilder contentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        public HttpRequestBuilder accept(String accept) {
            return accept(firstMediaType(accept));
        }

        public HttpRequestBuilder accept(MediaType accept) {
            this.accept = accept;
            return this;
        }

        public HttpRequestBuilder body(Object body) {
            if (body == null || body == JsonValue.NULL) return body(null);
            // JSON-B may produce a leading nl, but we want only a trailing nl
            return body(JSONB.toJson(body).trim() + "\n");
        }

        public HttpRequestBuilder body(String body) {
            this.body = body;
            return this;
        }
    }
}
