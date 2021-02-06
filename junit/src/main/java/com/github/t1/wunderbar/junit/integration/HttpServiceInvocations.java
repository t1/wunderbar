package com.github.t1.wunderbar.junit.integration;

import com.github.t1.wunderbar.junit.ExpectedResponseBuilder;
import com.github.t1.wunderbar.junit.Invocation;
import com.github.t1.wunderbar.junit.JUnitWunderBarException;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j @RequiredArgsConstructor
public class HttpServiceInvocations implements Invocations {
    private final String id;
    private final List<HttpServiceInvocation> invocations = new ArrayList<>();

    @Override public Object invoke(Method method, Object... args) throws Exception {
        for (var invocation : invocations)
            if (invocation.matches(method, args))
                return invocation.invoke();

        var invocation = createFor(method, args);
        invocations.add(invocation);
        ExpectedResponseBuilder.buildingInvocation = invocation;

        return invocation.nullValue();
    }

    private HttpServiceInvocation createFor(Method method, Object... args) {
        var declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(GraphQlClientApi.class))
            return new GraphQlInvocation(id, method, args);
        if (declaringClass.isAnnotationPresent(RegisterRestClient.class))
            return new RestInvocation(id, method, args);
        throw new JUnitWunderBarException("no technology recognized on " + declaringClass);
    }

    @Override public void done() {
        invocations.forEach(Invocation::done);
        invocations.clear();
    }
}
