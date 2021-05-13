package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j @RequiredArgsConstructor
public class IntegrationTestExpectations implements WunderBarExpectations {
    private final BarWriter bar;
    private final List<HttpServiceExpectation> expectations = new ArrayList<>();

    @Override public Object invoke(Method method, Object... args) {
        for (var expectation : expectations)
            if (expectation.matches(method, args))
                return expectation.invoke();

        var expectation = createFor(method, args);
        expectations.add(expectation);
        WunderbarExpectationBuilder.buildingExpectation = expectation;

        return expectation.nullValue();
    }

    private HttpServiceExpectation createFor(Method method, Object... args) {
        var declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(GraphQLClientApi.class))
            return new GraphQlExpectation(bar, method, args);
        if (declaringClass.isAnnotationPresent(RegisterRestClient.class))
            return new RestExpectation(bar, method, args);
        throw new WunderBarException("no technology recognized on " + declaringClass);
    }

    @Override public void done() {
        expectations.forEach(WunderBarExpectation::done);
        expectations.clear();
    }
}
