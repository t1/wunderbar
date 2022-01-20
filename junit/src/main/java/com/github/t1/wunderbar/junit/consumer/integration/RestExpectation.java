package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.lang.reflect.Method;

@Slf4j
class RestExpectation extends HttpServiceExpectation {
    RestExpectation(HttpServer server, Method method, Object... args) {super(server, method, args);}

    @Override protected Object service() {
        return RestClientBuilder.newBuilder().baseUri(baseUri().resolve("/rest")).build(method.getDeclaringClass());
    }

    @Override public HttpResponse handleRequest(HttpRequest request) {
        if (hasException()) return HttpResponse.problemDetail(getException());
        else return HttpResponse.builder().body(getResponse()).build();
    }
}
