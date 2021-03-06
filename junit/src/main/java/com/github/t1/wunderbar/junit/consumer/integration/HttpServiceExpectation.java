package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.Utils;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.Utils.formatJson;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static lombok.AccessLevel.PACKAGE;

abstract class HttpServiceExpectation extends WunderBarExpectation {
    private final HttpServer server;
    private Object service;

    @Getter(PACKAGE) private Object response;
    @Getter(PACKAGE) private Exception exception;

    HttpServiceExpectation(BarWriter bar, Method method, Object... args) {
        super(method, args);
        Function<HttpRequest, HttpResponse> handler = this::handleRequest;
        if (bar != null) handler = save(bar, handler);
        handler = this.formatRequestBody(handler);
        this.server = new HttpServer(handler);
    }

    abstract protected HttpResponse handleRequest(HttpRequest request);

    private Function<HttpRequest, HttpResponse> save(BarWriter bar, Function<HttpRequest, HttpResponse> handler) {
        return request -> {
            var response = handler.apply(request);
            bar.save(request, response);
            return response;
        };
    }

    private Function<HttpRequest, HttpResponse> formatRequestBody(Function<HttpRequest, HttpResponse> handler) {
        return request -> {
            if (request.getBody().isPresent() && APPLICATION_JSON_TYPE.isCompatible(request.getContentType()))
                request = request.withBody(Optional.of(formatJson(request.getBody().get())));
            return handler.apply(request);
        };
    }

    URI baseUri() { return server.baseUri(); }

    final Object invoke() {
        if (this.service == null) this.service = service();
        return Utils.invoke(service, method, args);
    }

    protected abstract Object service();

    @Override public void willReturn(Object response) {
        assertUnset("willReturn");
        this.response = response;
    }

    @Override public void willThrow(Exception exception) {
        assertUnset("willThrow");
        this.exception = exception;
    }

    private void assertUnset(String method) {
        assert response == null : "double " + method + " (response)";
        assert exception == null : "double " + method + " (exception)";
    }

    @SneakyThrows(IOException.class)
    @Override public void done() {
        if (service instanceof Closeable) ((Closeable) service).close();
        server.stop();
    }
}
