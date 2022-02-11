package com.github.t1.wunderbar.junit.http;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.With;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static com.github.t1.wunderbar.junit.http.HttpUtils.JSONB;
import static com.github.t1.wunderbar.junit.http.HttpUtils.PROBLEM_DETAIL_TYPE;
import static com.github.t1.wunderbar.junit.http.HttpUtils.formatJson;
import static com.github.t1.wunderbar.junit.http.HttpUtils.fromJson;
import static com.github.t1.wunderbar.junit.http.HttpUtils.isCompatible;
import static com.github.t1.wunderbar.junit.http.HttpUtils.optional;
import static com.github.t1.wunderbar.junit.http.HttpUtils.read;
import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;
import static javax.json.JsonValue.ValueType.OBJECT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static lombok.AccessLevel.NONE;

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
    /** internal, lazily converted json */
    @Getter(NONE) AtomicReference<Optional<JsonValue>> jsonValue = new AtomicReference<>();

    @JsonbCreator public HttpResponse(StatusType status, MediaType contentType, String body) {
        this.status = (status == null) ? OK : status;
        this.contentType = (contentType == null) ? APPLICATION_JSON_UTF8 : contentType;
        this.body = body;
    }

    @Override public String toString() {return (headerProperties() + body).trim();}

    public String headerProperties() {
        return "" +
               "Status: " + getStatusString() + "\n" +
               "Content-Type: " + contentType + "\n";
    }

    public String getStatusString() {return status.getStatusCode() + " " + status.getReasonPhrase();}

    public int getStatusCode() {return status.getStatusCode();}

    public HttpResponse withStatusCode(int statusCode) {
        var status = Status.fromStatusCode(statusCode);
        if (status == null) throw new IllegalArgumentException("undefined status code " + statusCode);
        return withStatus(status);
    }

    public boolean isProblemDetail() {
        return hasBody() && PROBLEM_DETAIL_TYPE.isCompatible(getContentType());
    }

    public HttpResponse withFormattedBody() {return (isJson()) ? withBody(body().map(HttpUtils::formatJson).orElseThrow()) : this;}

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

    public HttpResponse patch(Function<JsonPatchBuilder, JsonPatchBuilder> patch) {return with(patch.apply(Json.createPatchBuilder()));}

    public HttpResponse with(JsonPatchBuilder patch) {return with(patch.build());}

    public HttpResponse with(JsonPatch patch) {return with(patch.apply(jsonObject()));}

    public HttpResponse with(Function<JsonObjectBuilder, JsonObjectBuilder> mapper) {
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

    public HttpResponse with(JsonValue body) {return withBody(formatJson(body));}

    public Response toJaxRs() {
        return Response.status(getStatus()).type(getContentType()).entity(getBody()).build();
    }

    @SuppressWarnings("unused")
    public static class HttpResponseBuilder {
        public HttpResponseBuilder status(int status) {
            return status(Status.fromStatusCode(status));
        }

        public HttpResponseBuilder status(StatusType status) {
            this.status = status;
            return this;
        }


        public HttpResponseBuilder contentType(String contentType) {
            return contentType(MediaType.valueOf(contentType));
        }

        public HttpResponseBuilder contentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }


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
            if (body == null || body == JsonValue.NULL) return body(null);
            // JSON-B may produce a leading nl, but we want only a trailing nl
            return body(JSONB.toJson(body).trim() + "\n");
        }

        public HttpResponseBuilder body(String body) {
            this.body = body;
            return this;
        }
    }
}
