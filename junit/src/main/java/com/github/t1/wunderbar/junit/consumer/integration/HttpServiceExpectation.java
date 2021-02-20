package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.Utils;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder;
import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
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
        Function<HttpServerRequest, HttpServerResponse> handler = this::handleRequest;
        if (bar != null) handler = save(bar, handler);
        handler = this.formatRequestBody(handler);
        this.server = new HttpServer(handler);
    }

    boolean matches(Method method, Object... args) {
        return method == this.method && Arrays.deepEquals(args, this.args);
    }

    abstract protected HttpServerResponse handleRequest(HttpServerRequest request);

    private Function<HttpServerRequest, HttpServerResponse> save(BarWriter bar, Function<HttpServerRequest, HttpServerResponse> handler) {
        return request -> {
            var response = handler.apply(request);
            bar.save(request, response);
            return response;
        };
    }

    private Function<HttpServerRequest, HttpServerResponse> formatRequestBody(Function<HttpServerRequest, HttpServerResponse> handler) {
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
        WunderbarExpectationBuilder.buildingExpectation = null;
    }

    @Override public void willThrow(Exception exception) {
        assertUnset("willThrow");
        this.exception = exception;
        WunderbarExpectationBuilder.buildingExpectation = null;
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
