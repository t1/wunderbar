package com.github.t1.wunderbar.junit.consumer.system;

import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder;
import com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client.GraphQLRequestBuilder;
import com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client.JaxRsTypesafeGraphQLClientBuilder;
import com.github.t1.wunderbar.mock.RequestMatcher;
import com.github.t1.wunderbar.mock.ResponseSupplier;
import io.smallrye.graphql.client.impl.typesafe.reflection.MethodInvocation;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.junit.Utils.errorCode;
import static com.github.t1.wunderbar.mock.GraphQLBodyMatcher.graphQlRequest;
import static com.github.t1.wunderbar.mock.GraphQLResponseSupplier.graphQlError;

@Slf4j
public class SystemTestExpectations implements WunderBarExpectations {
    private final Class<?> type;
    private final URI baseUri;
    private final Technology technology;
    private final BarFilter filter;
    private final Object api;
    private final WunderBarMockServerApi mock;
    private final List<Integer> createdExpectationIds = new ArrayList<>();

    public SystemTestExpectations(Class<?> type, URI baseUri, Technology technology, BarWriter bar) {
        this.type = type;
        this.baseUri = baseUri;
        this.technology = technology;
        this.filter = new BarFilter(bar);
        this.api = buildApi();
        this.mock = new JaxRsTypesafeGraphQLClientBuilder().endpoint(baseUri).build(WunderBarMockServerApi.class);
    }

    private Object buildApi() {
        log.info("system test endpoint: {}", baseUri);
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
        var expectation = createFor(method, args);
        WunderbarExpectationBuilder.buildingExpectation = expectation;

        return expectation.nullValue();
    }

    private WunderBarExpectation createFor(Method method, Object... args) {
        return new WunderBarExpectation(method, args) {
            @Override public URI baseUri() {return baseUri;}

            @Override public void willReturn(Object response) {
            }

            @Override public void willThrow(Exception exception) {
                var result = mock.addWunderBarExpectation(matcher(method, args), response(exception));
                if (!"ok".equals(result.getStatus())) throw new IllegalStateException("expected status=ok, but got " + result);
                createdExpectationIds.add(result.getId());
            }
        };
    }

    private RequestMatcher matcher(Method method, Object... args) {
        var requestBuilder = new GraphQLRequestBuilder(MethodInvocation.of(method, args));
        return graphQlRequest()
            .query(requestBuilder.query())
            .variables(requestBuilder.variables())
            .build();
    }

    private ResponseSupplier response(Exception exception) {
        return graphQlError(errorCode(exception), exception.getMessage());
    }

    @SneakyThrows(IOException.class)
    @Override public void done() {
        while (!createdExpectationIds.isEmpty()) mock.removeWunderBarExpectation(createdExpectationIds.remove(0));
        if (api instanceof Closeable) ((Closeable) api).close();
    }

    @GraphQLClientApi
    private interface WunderBarMockServerApi {
        @Mutation WunderBarStubbingResult addWunderBarExpectation(@NonNull RequestMatcher matcher, @NonNull ResponseSupplier responseSupplier);
        @SuppressWarnings("UnusedReturnValue") @Mutation String removeWunderBarExpectation(int id);
    }

    @Data
    private static class WunderBarStubbingResult {
        int id;
        String status;
    }
}
