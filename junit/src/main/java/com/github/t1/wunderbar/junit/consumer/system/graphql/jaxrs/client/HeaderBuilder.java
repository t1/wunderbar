package com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client;

import io.smallrye.graphql.client.GraphQLClientException;
import io.smallrye.graphql.client.impl.typesafe.reflection.MethodInvocation;
import io.smallrye.graphql.client.impl.typesafe.reflection.MethodResolver;
import io.smallrye.graphql.client.impl.typesafe.reflection.TypeInfo;
import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.Header;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.ws.rs.core.MultivaluedMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Base64;
import java.util.Map;

import static com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client.JaxRsCollectionUtils.toMultivaluedMap;
import static java.nio.charset.StandardCharsets.UTF_8;

// TODO remove this after #1222 is fixed
public class HeaderBuilder {
    private final Class<?> api;
    private final MethodInvocation method;
    private final Map<String, String> additionalHeaders;

    public HeaderBuilder(Class<?> api, MethodInvocation method, Map<String, String> additionalHeaders) {
        this.api = api;
        this.method = method;
        this.additionalHeaders = additionalHeaders;
    }

    public MultivaluedMap<String, Object> build() {
        MultivaluedMap<String, Object> headers = method.getResolvedAnnotations(api, Header.class)
            .map(header -> new SimpleEntry<>(header.name(), resolveValue(header)))
            .collect(toMultivaluedMap());
        method.headerParameters().forEach(parameter -> {
            Header header = parameter.getAnnotations(Header.class)[0];
            headers.add(header.name(), parameter.getValue());
        });
        method.getResolvedAnnotations(api, AuthorizationHeader.class)
            .findFirst()
            .map(header -> resolveAuthHeader(method.getDeclaringType(), header))
            .ifPresent(auth -> headers.add("Authorization", auth));
        if (additionalHeaders != null) {
            additionalHeaders.forEach(headers::putSingle);
        }
        return headers;
    }

    private Object resolveValue(Header header) {
        if (!header.method().isEmpty()) {
            if (!header.constant().isEmpty())
                throw new IllegalStateException("Header with 'method' AND 'constant' not allowed: " + header);
            return resolveMethodValue(header.method());
        }
        if (header.constant().isEmpty())
            throw new IllegalStateException("Header must have either 'method' XOR 'constant': " + header);
        return header.constant();
    }

    private Object resolveMethodValue(String methodName) {
        TypeInfo declaringType = method.getDeclaringType();
        MethodInvocation method = new MethodResolver(declaringType, methodName).resolve();
        if (!method.isStatic())
            throw new IllegalStateException("referenced header method '" + methodName + "'" +
                                            " in " + declaringType.getTypeName() + " is not static");
        try {
            return method.invoke(null).toString();
        } catch (RuntimeException e) {
            if (e instanceof GraphQLClientException)
                throw e;
            throw new IllegalStateException("can't resolve header method expression '" + methodName + "'" +
                                            " in " + declaringType.getTypeName(), e);
        }
    }

    private String resolveAuthHeader(TypeInfo declaringType, AuthorizationHeader header) {
        if (header.confPrefix().isEmpty())
            return auth(header.type(), declaringType.getRawType());
        return auth(header.type(), header.confPrefix());
    }

    private static String auth(AuthorizationHeader.Type type, Class<?> api) {
        return auth(type, configKey(api));
    }

    private static String configKey(Class<?> api) {
        GraphQLClientApi annotation = api.getAnnotation(GraphQLClientApi.class);
        if (annotation == null || annotation.configKey().isEmpty())
            return api.getName();
        return annotation.configKey();
    }

    private static String auth(AuthorizationHeader.Type type, String configKey) {
        String prefix;
        if (configKey.endsWith("*"))
            prefix = configKey.substring(0, configKey.length() - 1);
        else
            prefix = configKey + "/mp-graphql/";
        switch (type) {
            case BASIC:
                return basic(prefix);
            case BEARER:
                return bearer(prefix);
        }
        throw new UnsupportedOperationException("unreachable");
    }

    private static String basic(String prefix) {
        String username = CONFIG.getValue(prefix + "username", String.class);
        String password = CONFIG.getValue(prefix + "password", String.class);
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(UTF_8));
    }

    private static String bearer(String prefix) {
        return "Bearer " + CONFIG.getValue(prefix + "bearer", String.class);
    }

    private static final Config CONFIG = ConfigProvider.getConfig();
}
