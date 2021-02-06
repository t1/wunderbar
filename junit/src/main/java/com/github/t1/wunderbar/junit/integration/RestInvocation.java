package com.github.t1.wunderbar.junit.integration;

import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.StatusType;
import java.lang.reflect.Method;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Slf4j
class RestInvocation extends HttpServiceInvocation {

    RestInvocation(String id, Method method, Object... args) { super(id, method, args); }

    @Override protected Object service() {
        return RestClientBuilder.newBuilder().baseUri(baseUri()).build(method.getDeclaringClass());
    }

    @Override protected HttpServerResponse handleRequest(HttpServerRequest request) {
        var out = HttpServerResponse.builder();
        var exception = getException();
        if (exception == null) {
            out.body(getResponse());
        } else {
            out.status(statusOf(exception));
            out.body(ProblemDetails.of(exception));
            out.contentType(PROBLEM_DETAILS_TYPE);
        }
        return out.build();
    }

    private StatusType statusOf(Exception exception) {
        return (exception instanceof WebApplicationException)
            ? ((WebApplicationException) exception).getResponse().getStatusInfo()
            : INTERNAL_SERVER_ERROR;
    }

    @Builder @Getter
    public static class ProblemDetails {
        private static ProblemDetails of(Exception exception) {
            return ProblemDetails.builder()
                .type(URI.create("urn:problem-type:" + exception.getClass().getName()))
                .title(exception.getClass().getSimpleName())
                .detail(exception.getMessage())
                .build();
        }

        URI type;
        String title;
        String detail;
    }

    private static final MediaType PROBLEM_DETAILS_TYPE = MediaType.valueOf("application/problem+json");
}
