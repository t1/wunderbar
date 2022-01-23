package com.github.t1.wunderbar.junit.http;

import lombok.Builder;
import lombok.Value;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.StatusType;
import java.net.URI;

import static com.github.t1.wunderbar.junit.http.HttpUtils.PROBLEM_DETAIL_TYPE;
import static com.github.t1.wunderbar.junit.http.HttpUtils.errorCode;
import static com.github.t1.wunderbar.junit.http.HttpUtils.title;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Value @Builder
public class ProblemDetails {
    public static ProblemDetails of(Exception exception) {
        return ProblemDetails.builder()
            .status(statusOf(exception))
            .type(URI.create("urn:problem-type:" + errorCode(exception)))
            .title(title(exception))
            .detail(exception.getMessage())
            .build();
    }

    private static StatusType statusOf(Exception exception) {
        return (exception instanceof WebApplicationException)
            ? ((WebApplicationException) exception).getResponse().getStatusInfo()
            : null;
    }

    StatusType status; // this is optional in the spec, but we use it to transport the status to the HttpResponse
    URI type;
    String title;
    String detail;

    public HttpResponse toResponse() {
        return HttpResponse.builder()
            .status((status == null) ? INTERNAL_SERVER_ERROR : status)
            .contentType(PROBLEM_DETAIL_TYPE)
            .body(this)
            .build();
    }
}
