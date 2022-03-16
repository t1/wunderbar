package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.consumer.Depletion;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

import static lombok.AccessLevel.PACKAGE;

@Slf4j
public abstract @Internal class HttpServiceExpectation extends WunderBarExpectation {
    public static HttpServiceExpectation of(Technology technology, URI baseUri, Method method, Object... args) {
        switch (technology) {
            case GRAPHQL:
                return new GraphQlExpectation(baseUri, method, args);
            case REST:
                return new RestExpectation(baseUri, method, args);
        }
        throw new UnsupportedOperationException("unreachable");
    }

    private final URI baseUri;
    private Object service;
    private AfterStubbing afterStubbing = () -> {};

    @Getter(PACKAGE) private Object response;
    @Getter(PACKAGE) private Exception exception;
    @Getter private Depletion depletion;
    private int callCount = 0;

    HttpServiceExpectation(URI baseUri, Method method, Object... args) {
        super(method, args);
        this.baseUri = baseUri;
    }

    public HttpServiceExpectation afterStubbing(AfterStubbing afterStubbing) {
        this.afterStubbing = afterStubbing;
        return this;
    }

    public abstract HttpResponse handleRequest(HttpRequest request);

    @Override public URI baseUri() {return baseUri;}

    public boolean hasException() {return exception != null;}

    public final Object invoke() {
        ++callCount;
        log.debug("invocation #{} of {}", callCount, this);
        checkDepletion();
        if (this.service == null) this.service = service();
        return Utils.invoke(service, method, args);
    }

    protected abstract Object service();

    @Override public void returns(@NonNull Depletion depletion, @NonNull Object response) {
        assertUnset("returns");
        this.depletion = depletion;
        this.response = response;
        afterStubbing.run();
    }

    @Override public void willThrow(@NonNull Depletion depletion, @NonNull Exception exception) {
        assertUnset("willThrow");
        this.depletion = depletion;
        this.exception = exception;
        afterStubbing.run();
    }

    private void assertUnset(String method) {
        assert response == null : "double " + method + " (response)";
        assert exception == null : "double " + method + " (exception)";
    }

    public void checkDepletion() {
        depletion.check(callCount);
    }

    @SneakyThrows(IOException.class)
    @Override public void done() {
        if (service instanceof Closeable) ((Closeable) service).close();
    }
}
