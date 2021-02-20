package com.github.t1.wunderbar.junit.consumer.system;

import com.github.t1.wunderbar.junit.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import io.smallrye.graphql.client.typesafe.impl.GraphQlClientBuilderImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.client.ClientBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

@Slf4j
public class SystemExpectations implements WunderBarExpectations {
    private final Object api;
    private final BarFilter filter;

    public SystemExpectations(Class<?> type, String endpoint, BarWriter bar) {
        this.filter = new BarFilter(bar);
        this.api = buildApi(type, endpoint);
    }

    private Object buildApi(Class<?> type, String endpointTemplate) {
        if (type.isAnnotationPresent(GraphQlClientApi.class))
            return ((GraphQlClientBuilderImpl) GraphQlClientBuilder.newBuilder())
                .client(ClientBuilder.newClient()) // TODO remove after https://github.com/smallrye/smallrye-graphql/issues/634
                .endpoint(resolve(endpointTemplate, "graphql"))
                .register(filter)
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
