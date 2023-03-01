package com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client;

import io.smallrye.graphql.client.impl.GraphQLClientConfiguration;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;
import io.smallrye.graphql.client.impl.typesafe.reflection.MethodInvocation;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import io.smallrye.graphql.client.websocket.WebsocketSubprotocol;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedHashMap;
import java.util.Map;

public class JaxRsTypesafeGraphQLClientBuilder implements TypesafeGraphQLClientBuilder {
    private String configKey = null;
    private Client client;
    private URI endpoint;
    private Map<String, String> extraHeaders;
    private boolean allowUnexpectedResponseFields;

    @Override
    public TypesafeGraphQLClientBuilder configKey(String configKey) {
        this.configKey = configKey;
        return this;
    }

    public TypesafeGraphQLClientBuilder client(Client client) {
        this.client = client;
        return this;
    }

    private Client client() {
        if (client == null)
            client = ClientBuilder.newClient();
        return client;
    }

    @Override
    public TypesafeGraphQLClientBuilder endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    @Override public TypesafeGraphQLClientBuilder websocketUrl(String s) {
        throw new UnsupportedOperationException();
    }

    @Override public TypesafeGraphQLClientBuilder websocketInitializationTimeout(Integer integer) {
        throw new UnsupportedOperationException();
    }

    @Override public TypesafeGraphQLClientBuilder executeSingleOperationsOverWebsocket(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override public TypesafeGraphQLClientBuilder subprotocols(WebsocketSubprotocol... websocketSubprotocols) {
        throw new UnsupportedOperationException();
    }

    @Override public TypesafeGraphQLClientBuilder allowUnexpectedResponseFields(boolean value) {
        this.allowUnexpectedResponseFields = value;
        return this;
    }


    @Override public TypesafeGraphQLClientBuilder header(String name, String value) {
        if (this.extraHeaders == null) {
            this.extraHeaders = new LinkedHashMap<>();
        }

        this.extraHeaders.put(name, value);
        return this;
    }

    @Override public TypesafeGraphQLClientBuilder initPayload(Map<String, Object> initPayload) {
        return this;
    }

    public TypesafeGraphQLClientBuilder register(Object component) {
        client().register(component);
        return this;
    }

    @Override
    public <T> T build(Class<T> apiClass) {
        if (configKey == null) {
            configKey = configKey(apiClass);
        }

        GraphQLClientsConfiguration configs = GraphQLClientsConfiguration.getInstance();
        configs.initTypesafeClientApi(apiClass);
        GraphQLClientConfiguration persistentConfig = configs.getClient(configKey);
        if (persistentConfig != null) applyConfig(persistentConfig);

        WebTarget webTarget = client().target(endpoint);
        JaxRsTypesafeGraphQLClientProxy graphQlClient = new JaxRsTypesafeGraphQLClientProxy(webTarget, persistentConfig, extraHeaders, allowUnexpectedResponseFields);
        return apiClass.cast(Proxy.newProxyInstance(getClassLoader(apiClass), new Class<?>[]{apiClass},
            (proxy, method, args) -> invoke(apiClass, graphQlClient, method, args)));
    }

    private Object invoke(Class<?> apiClass, JaxRsTypesafeGraphQLClientProxy graphQlClient, java.lang.reflect.Method method,
                          Object... args) {
        MethodInvocation methodInvocation = MethodInvocation.of(method, args);
        if (methodInvocation.isDeclaredInCloseable()) {
            client().close();
            return null; // void
        }
        return graphQlClient.invoke(apiClass, methodInvocation);
    }

    private ClassLoader getClassLoader(Class<?> apiClass) {
        if (System.getSecurityManager() == null)
            return apiClass.getClassLoader();
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) apiClass::getClassLoader);
    }

    /**
     * Applies values from known global configuration. This does NOT override values passed to this
     * builder by method calls.
     */
    private void applyConfig(GraphQLClientConfiguration configuration) {
        if (this.endpoint == null && configuration.getUrl() != null) {
            this.endpoint = URI.create(configuration.getUrl());
        }
    }

    private String configKey(Class<?> apiClass) {
        GraphQLClientApi annotation = apiClass.getAnnotation(GraphQLClientApi.class);
        if (annotation == null) {
            return apiClass.getName();
        }
        String keyFromAnnotation = annotation.configKey();
        return (keyFromAnnotation.isEmpty()) ? apiClass.getName() : keyFromAnnotation;
    }
}
