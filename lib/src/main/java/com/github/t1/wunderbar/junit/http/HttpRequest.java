package com.github.t1.wunderbar.junit.http;

import com.github.t1.wunderbar.common.Internal;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonValue;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.ws.rs.core.MediaType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.common.Utils.nonAddFieldDiff;
import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static com.github.t1.wunderbar.junit.http.HttpUtils.JSONB;
import static com.github.t1.wunderbar.junit.http.HttpUtils.firstMediaType;
import static com.github.t1.wunderbar.junit.http.HttpUtils.formatJson;
import static com.github.t1.wunderbar.junit.http.HttpUtils.isCompatible;
import static com.github.t1.wunderbar.junit.http.HttpUtils.mediaTypes;
import static com.github.t1.wunderbar.junit.http.HttpUtils.normalizeTitle;
import static com.github.t1.wunderbar.junit.http.HttpUtils.read;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.WILDCARD_TYPE;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.NONE;

@Value @Builder @With @EqualsAndHashCode(exclude = "jsonValue")
public class HttpRequest {
    @NonNull String method;
    @NonNull String uri;
    Authorization authorization;
    @NonNull MediaType contentType;
    List<MediaType> accept;
    List<Header> headers;
    String body;
    /** internal, lazily converted json */
    @Getter(NONE) AtomicReference<Optional<JsonValue>> jsonValue = new AtomicReference<>();

    @Internal @JsonbCreator public HttpRequest(
            String method,
            String uri,
            Authorization authorization,
            MediaType contentType,
            List<MediaType> accept,
            List<Header> headers,
            String body
    ) {
        this.method = (method == null) ? "GET" : method;
        this.uri = (uri == null) ? "/" : uri;
        this.contentType = (contentType == null) ? APPLICATION_JSON_UTF8 : contentType;
        this.accept = (accept == null) ? List.of(APPLICATION_JSON_UTF8) : accept;
        this.authorization = authorization;
        this.headers = headers;
        this.body = body;
    }

    @Override public String toString() {return (method + " " + uri + "\n" + body().orElse("")).trim();}

    public String headerProperties() {
        return ((accept == null) ? "" : ACCEPT + ": " + accept() + "\n") +
               (CONTENT_TYPE + ": " + contentType + "\n") +
               ((authorization == null) ? "" : AUTHORIZATION + ": " + authorization + "\n") +
               ((headers == null) ? "" : headers.stream().map(Header::toString).collect(joining("\n")) + "\n");
    }

    public String accept() {
        return (accept.size() == 1) ? accept.get(0).toString()
                : accept.stream().map(MediaType::toString).collect(joining("; "));
    }

    public URI getUri() {return URI.create(uri);}

    public String uri() {return uri;}

    public HttpRequest withoutContextPath() {
        var contextPath = getContextPath();
        return withUri(uri().substring(contextPath.length()));
    }

    /** The first part of the path, a.k.a. servlet name or context path. */
    public String getContextPath() {return "/" + Path.of(uri()).getName(0);}

    public MatchResult matchUri(String pattern) {return matchUri(Pattern.compile(pattern));}

    public MatchResult matchUri(Pattern pattern) {
        var matcher = pattern.matcher(uri());
        if (!matcher.matches())
            throw new IllegalArgumentException("expected uri to match " + pattern + " but was " + uri);
        return matcher.toMatchResult(); // make immutable
    }

    /**
     * Almost the same as <code>equals</code>, but
     * the content types only have to be compatible, and
     * <code>that</code> body may contain more fields than <code>this</code> does.
     */
    public boolean matches(HttpRequest that) {
        return this.method.equals(that.method)
               && this.uri.equals(that.uri)
               && (this.authorization == null || this.authorization.equals(that.authorization))
               && isCompatible(this.contentType, that.contentType)
               && (this.accept == null || isCompatible(this.accept, that.accept))
               && (this.body == null || matchesBody(that));
    }

    private boolean matchesBody(HttpRequest that) {
        if (!this.isJson() || !that.isJson()) return false;
        return nonAddFieldDiff(this.jsonValue(), that.jsonValue()).findAny().isEmpty();
    }

    public HttpRequest normalized() {
        var out = this;
        if (isJson()) out = withBody(body().map(HttpUtils::formatJson).orElseThrow());
        if (WILDCARD_TYPE.equals(contentType)) out = out.withContentType(APPLICATION_JSON_UTF8);
        return out;
    }

    public boolean hasBody() {return body != null;}

    public Optional<String> body() {return Optional.ofNullable(body);}

    public Optional<JsonValue> json() {return jsonValue.updateAndGet(this::createOrGetJsonValue);}

    public <T> T as(Class<T> type) {return read(body, contentType, type);}

    public boolean isJson() {return hasBody() && isCompatible(APPLICATION_JSON_TYPE, contentType);}

    public boolean isJsonObject() {return isJson() && jsonValue().getValueType() == OBJECT;}

    private Optional<JsonValue> createOrGetJsonValue(Optional<JsonValue> old) {
        //noinspection OptionalAssignedToNull // not initialized
        return (old == null) ? body().map(HttpUtils::readJson) : old;
    }

    public JsonValue jsonValue() {return json().orElseThrow();}

    public JsonObject jsonObject() {return jsonValue().asJsonObject();}

    public boolean has(String field) {return isJsonObject() && jsonObject().containsKey(field);}

    public JsonValue get(String pointer) {
        if (!pointer.startsWith("/")) pointer = "/" + pointer;
        return jsonObject().getValue(pointer);
    }

    public HttpRequest patch(Function<JsonPatchBuilder, JsonPatchBuilder> patch) {return with(patch.apply(Json.createPatchBuilder()));}

    public HttpRequest with(JsonPatchBuilder patch) {return with(patch.build());}

    public HttpRequest with(JsonPatch patch) {return with(patch.apply(jsonObject()));}

    public HttpRequest with(Function<JsonObjectBuilder, JsonObjectBuilder> mapper) {
        return with(mapper.apply(getObjectBuilder()).build());
    }

    private JsonObjectBuilder getObjectBuilder() {
        if (hasBody()) {
            assert isJsonObject();
            return Json.createObjectBuilder(jsonObject());
        } else {
            return Json.createObjectBuilder();
        }
    }

    public HttpRequest with(JsonValue body) {return withBody(formatJson(body));}

    @NoArgsConstructor(force = true)
    public static @Value class Header {
        @NonNull String name;
        @NonNull List<String> values;

        public Header(@NonNull String name, @NonNull List<String> values) {
            this.name = normalizeTitle(name);
            this.values = values;
        }

        @Override public String toString() {return (values.isEmpty()) ? "" : name + ": " + String.join("; ", values);}
    }

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
            if (accept != null && accept.startsWith("[") && accept.endsWith("]"))
                accept = accept.substring(1, accept.length() - 1);
            return accept(mediaTypes(accept).toArray(MediaType[]::new));
        }

        public HttpRequestBuilder accept(MediaType... mediaTypes) {
            if (this.accept == null) this.accept = new ArrayList<>();
            this.accept.addAll(List.of(mediaTypes));
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

        public HttpRequestBuilder header(String name, List<String> values) {
            switch (name) {
                case AUTHORIZATION:
                    assert values.size() == 1;
                    authorization(Authorization.valueOf(values.get(0)));
                    break;
                case ACCEPT:
                    values.forEach(this::accept);
                    break;
                case CONTENT_TYPE:
                    assert values.size() == 1;
                    contentType(values.get(0));
                    break;
                default:
                    if (this.headers == null) this.headers = new ArrayList<>();
                    this.headers.add(new Header(name, unmodifiableList(values)));
            }
            return this;
        }
    }
}
