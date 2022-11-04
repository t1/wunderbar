package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.ProblemDetails;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.lang.reflect.Method;
import java.net.URI;

import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;

@Slf4j
class RestExpectation extends HttpServiceExpectation {
    RestExpectation(URI baseUri, Method method, Object... args) {super(baseUri, method, args);}

    @Override protected Object service() {
        return RestClientBuilder.newBuilder().baseUri(baseUri()).build(method.getDeclaringClass());
    }

    @Override public HttpResponse handleRequest(HttpRequest request) {
        if (hasException()) return ProblemDetails.of(getException()).toResponse();
        else return HttpResponse.builder().body(getResponse()).contentType(contentType()).build();
    }

    private MediaType contentType() {
        return method.isAnnotationPresent(Produces.class)
            ? MediaType.valueOf(method.getAnnotation(Produces.class).value()[0])
            : APPLICATION_JSON_UTF8;
    }
}
