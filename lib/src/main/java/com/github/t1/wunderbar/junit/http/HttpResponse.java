package com.github.t1.wunderbar.junit.http;

import com.github.t1.wunderbar.common.Utils.ProblemDetails;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.With;

import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import java.util.Optional;
import java.util.Properties;

import static com.github.t1.wunderbar.common.Utils.PROBLEM_DETAILS_TYPE;
import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static com.github.t1.wunderbar.junit.http.HttpUtils.JSONB;
import static com.github.t1.wunderbar.junit.http.HttpUtils.optional;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Value @Builder @With
public class HttpResponse {
    public static HttpResponse problemDetail(Exception exception) {
        var builder = HttpResponse.builder();
        if (exception == null) exception = new RuntimeException();
        builder.status(statusOf(exception));
        builder.body(ProblemDetails.of(exception));
        builder.contentType(PROBLEM_DETAILS_TYPE);
        return builder.build();
    }

    private static StatusType statusOf(Exception exception) {
        return (exception instanceof WebApplicationException)
            ? ((WebApplicationException) exception).getResponse().getStatusInfo()
            : INTERNAL_SERVER_ERROR;
    }

    public static HttpResponse from(Properties properties, Optional<String> body) {
        var builder = HttpResponse.builder();
        optional(properties, "Status").map(HttpUtils::toStatus).ifPresent(builder::status);
        optional(properties, "Content-Type").map(MediaType::valueOf).ifPresent(builder::contentType);
        body.ifPresent(builder::body);
        return builder.build();
    }

    @Default StatusType status = OK;
    @Default MediaType contentType = APPLICATION_JSON_UTF8;
    @Default Optional<String> body = Optional.empty();

    @Override public String toString() {return (headerProperties() + body.orElse("")).trim();}

    public String headerProperties() {
        return "" +
               "Status: " + getStatusString() + "\n" +
               "Content-Type: " + contentType + "\n";
    }

    public String getStatusString() {return status.getStatusCode() + " " + status.getReasonPhrase();}

    public Optional<JsonValue> getJsonBody() {return body.map(HttpUtils::toJson);}

    @SuppressWarnings("unused")
    public static class HttpResponseBuilder {
        public HttpResponseBuilder body(Object body) {
            // JSON-B may produce a leading nl, but we want only a trailing nl
            return body(JSONB.toJson(body).trim() + "\n");
        }

        public HttpResponseBuilder body(String body) {
            body$value = Optional.of(body);
            body$set = true;
            return this;
        }

        public HttpResponseBuilder status(int status) {
            return status(Status.fromStatusCode(status));
        }

        public HttpResponseBuilder status(StatusType status) {
            this.status$value = status;
            this.status$set = true;
            return this;
        }
    }
}
