package com.github.t1.wunderbar.junit.consumer.system;

import com.github.t1.wunderbar.junit.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.lang.reflect.Method;
import java.net.URI;

@Slf4j
public class SystemExpectations implements WunderBarExpectations {
    private final Object api;

    public SystemExpectations(Class<?> type, String endpoint) {
        this.api = buildApi(type, endpoint);
    }

    private static Object buildApi(Class<?> type, String endpointTemplate) {
        if (type.isAnnotationPresent(GraphQlClientApi.class))
            return GraphQlClientBuilder.newBuilder().endpoint(resolve(endpointTemplate, "graphql")).build(type);
        if (type.isAnnotationPresent(RegisterRestClient.class))
            return RestClientBuilder.newBuilder().baseUri(resolve(endpointTemplate, "rest")).build(type);
        throw new WunderBarException("can't determine technology of API " + type.getName());
    }

    private static URI resolve(String template, String technology) {
        var uri = URI.create(template.replace("{technology}", technology));
        log.info("resolve system test endpoint to {}", uri);
        return uri;
    }

    @Override public Object invoke(Method method, Object... args) {
        return Utils.invoke(api, method, args);
    }
}
