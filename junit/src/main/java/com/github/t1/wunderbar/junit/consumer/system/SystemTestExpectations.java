package com.github.t1.wunderbar.junit.consumer.system;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client.JaxRsTypesafeGraphQLClientBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

@Slf4j
public class SystemTestExpectations implements WunderBarExpectations {
    private final Object api;
    private final BarFilter filter;
    private final URI baseUri;

    public SystemTestExpectations(Class<?> type, URI endpoint, Technology technology, BarWriter bar) {
        this.filter = new BarFilter(bar);
        this.baseUri = endpoint;
        this.api = buildApi(type, endpoint, technology);
    }

    private Object buildApi(Class<?> type, URI endpoint, Technology technology) {
        log.info("system test endpoint: {}", endpoint);
        switch (technology) {
            case GRAPHQL:
                return new JaxRsTypesafeGraphQLClientBuilder()
                    .register(filter)
                    .endpoint(baseUri)
                    .build(type);
            case REST:
                return RestClientBuilder.newBuilder()
                    .baseUri(baseUri)
                    .register(filter)
                    .build(type);
        }
        throw new UnsupportedOperationException("unreachable");
    }

    @Override public URI baseUri() {return baseUri;}

    @Override public Object asSutProxy(Object proxy) {return api;}

    @Override public Object invoke(Method method, Object... args) {
        throw new WunderBarException("can't stub system tests");
    }

    @SneakyThrows(IOException.class)
    @Override public void done() {
        if (api instanceof Closeable) ((Closeable) api).close();
    }
}
