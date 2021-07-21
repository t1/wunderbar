package com.github.t1.wunderbar.junit.consumer.system;

import com.github.t1.wunderbar.junit.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import io.smallrye.graphql.client.typesafe.jaxrs.JaxRsTypesafeGraphQLClientBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

@Slf4j
public class SystemTestExpectations implements WunderBarExpectations {
    private final Object api;
    private final BarFilter filter;

    public SystemTestExpectations(Class<?> type, String endpointTemplate, BarWriter bar) {
        this.filter = new BarFilter(bar);
        this.api = buildApi(type, endpointTemplate);
    }

    private Object buildApi(Class<?> type, String endpointTemplate) {
        if (type.isAnnotationPresent(GraphQLClientApi.class))
            return ((JaxRsTypesafeGraphQLClientBuilder) TypesafeGraphQLClientBuilder.newBuilder())
                .register(filter)
                .endpoint(resolve(endpointTemplate, "graphql"))
                .build(type);
        if (type.isAnnotationPresent(RegisterRestClient.class))
            return RestClientBuilder.newBuilder()
                .baseUri(resolve(endpointTemplate, "rest"))
                .register(filter)
                .build(type);
        throw new WunderBarException("can't determine technology of API " + type.getName());
    }

    private static URI resolve(String template, String technology) {
        var uri = URI.create(template.replace("{technology}", technology));
        log.info("system test endpoint: {}", uri);
        return uri;
    }

    @Override public Object invoke(Method method, Object... args) {
        return Utils.invoke(api, method, args);
    }

    @SneakyThrows(IOException.class)
    @Override public void done() {
        if (api instanceof Closeable) ((Closeable) api).close();
    }
}
