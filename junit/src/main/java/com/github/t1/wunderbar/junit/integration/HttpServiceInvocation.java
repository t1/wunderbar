package com.github.t1.wunderbar.junit.integration;

import com.github.t1.wunderbar.junit.Bar;
import com.github.t1.wunderbar.junit.ExpectedResponseBuilder;
import com.github.t1.wunderbar.junit.Invocation;
import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;

import static lombok.AccessLevel.PACKAGE;

abstract class HttpServiceInvocation extends Invocation {
    private final String id;

    private final HttpServer server;

    @Getter(PACKAGE) private Object response;
    @Getter(PACKAGE) private Exception exception;

    public HttpServiceInvocation(String id, Method method, Object... args) {
        super(method, args);
        this.id = id;
        this.server = new HttpServer(Bar.save(id, this::handleRequest));
    }

    boolean matches(Method method, Object... args) {
        return method == this.method && Arrays.deepEquals(args, this.args);
    }

    abstract protected HttpServerResponse handleRequest(HttpServerRequest request);

    URI baseUri() { return server.baseUri(); }

    final Object invoke() throws Exception {
        method.setAccessible(true);
        try {
            return method.invoke(service(), args);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException)
                throw (RuntimeException) e.getTargetException();
            throw e;
        }
    }

    protected abstract Object service();

    @Override public void willReturn(Object response) {
        assertUnset("willReturn");
        this.response = response;
        ExpectedResponseBuilder.buildingInvocation = null;
    }

    @Override public void willThrow(Exception exception) {
        assertUnset("willThrow");
        this.exception = exception;
        ExpectedResponseBuilder.buildingInvocation = null;
    }

    private void assertUnset(String method) {
        assert response == null : "double " + method + " (response)";
        assert exception == null : "double " + method + " (exception)";
    }

    @Override public void done() { server.stop(); }
}
