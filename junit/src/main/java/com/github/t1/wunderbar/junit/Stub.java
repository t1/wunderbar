package com.github.t1.wunderbar.junit;

import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@RequiredArgsConstructor @ToString
abstract class Stub implements Closeable {
    static Stub on(Method method, Object... args) {
        var declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(GraphQlClientApi.class))
            return new GraphQlStub(method, args);
        if (declaringClass.isAnnotationPresent(RegisterRestClient.class))
            return new RestStub(method, args);
        throw new JUnitWunderBarException("no technology recognized on " + declaringClass);
    }


    protected final @NonNull Method method;
    protected final @NonNull Object[] args;
    protected Object response;
    protected Exception exception;

    void assertUnset(String method) {
        assert response == null : "double " + method + " (response)";
        assert exception == null : "double " + method + " (exception)";
    }

    boolean matches(Method method, Object... args) {
        return method == this.method && Arrays.deepEquals(args, this.args);
    }

    abstract Object invoke() throws Exception;

    protected Object invokeOn(Object instance) throws Exception {
        method.setAccessible(true);
        try {
            return method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException)
                throw (RuntimeException) e.getTargetException();
            throw e;
        }
    }

    // default implementation and without `throws IOException`
    @Override public void close() {}

    String name() {
        return method.toGenericString() +
            Stream.of(args).map(Object::toString).collect(joining(", ", " [", "]"));
    }
}
