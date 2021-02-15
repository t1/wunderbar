package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.Bar;
import com.github.t1.wunderbar.junit.Utils;
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
import java.util.function.Function;

import static lombok.AccessLevel.PACKAGE;

abstract class HttpServiceExpectation extends WunderBarExpectation {
    private final HttpServer server;
    private Object service;

    @Getter(PACKAGE) private Object response;
    @Getter(PACKAGE) private Exception exception;

    HttpServiceExpectation(Bar bar, Method method, Object... args) {
        super(method, args);
        Function<HttpServerRequest, HttpServerResponse> handler = this::handleRequest;
        if (bar != null) handler = bar.save(handler);
        this.server = new HttpServer(handler);
    }

    boolean matches(Method method, Object... args) {
        return method == this.method && Arrays.deepEquals(args, this.args);
    }

    abstract protected HttpServerResponse handleRequest(HttpServerRequest request);

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
