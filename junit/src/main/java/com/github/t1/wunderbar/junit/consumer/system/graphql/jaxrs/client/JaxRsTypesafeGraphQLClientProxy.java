package com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client;

import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.typesafe.HeaderBuilder;
import io.smallrye.graphql.client.impl.typesafe.ResultBuilder;
import io.smallrye.graphql.client.impl.typesafe.reflection.MethodInvocation;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.WebTarget;
import java.util.Collections;
import java.util.Map;

import static com.github.t1.wunderbar.common.Utils.prefix;
import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

@Slf4j
class JaxRsTypesafeGraphQLClientProxy {
    private final WebTarget target;
    private final GraphQLClientConfiguration configuration;

    JaxRsTypesafeGraphQLClientProxy(WebTarget target, GraphQLClientConfiguration configuration) {
        this.target = target;
        this.configuration = configuration;
    }

    Object invoke(Class<?> api, MethodInvocation method) {
        if (method.isDeclaredInObject())
            return method.invoke(this);
        log.debug("call {} to {}", method.getName(), target.getUri());

        var headers = new HeaderBuilder(api, method,
            configuration != null ? configuration.getHeaders() : Collections.emptyMap())
            .build();
        var request = new GraphQLRequestBuilder(method).build().toString();

        var response = post(request, headers);

        log.debug("response graphql:\n{}", prefix("    ", response));
        if (response == null || response.isBlank()) response = "{}";
        return new ResultBuilder(method, response).read();
    }

    private String post(String requestBody, Map<String, String> headers) {
        var request = target.request(APPLICATION_JSON_UTF8);
        headers.forEach(request::header);

        var response = request.post(entity(requestBody, APPLICATION_JSON_UTF8));

        var status = response.getStatusInfo();
        if (status.getFamily() != SUCCESSFUL)
            throw new IllegalStateException(
                "expected successful status code but got " +
                status.getStatusCode() + " " + status.getReasonPhrase() + ":\n" +
                response.readEntity(String.class));
        return response.readEntity(String.class);
    }
}
