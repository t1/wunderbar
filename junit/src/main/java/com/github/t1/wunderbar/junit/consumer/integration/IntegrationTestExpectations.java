package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Path;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.t1.wunderbar.junit.Utils.formatJson;
import static com.github.t1.wunderbar.junit.Utils.isCompatible;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;

@Slf4j
public class IntegrationTestExpectations implements WunderBarExpectations {
    private final BarWriter bar;

    @Getter private final HttpServer server;
    private final List<HttpServiceExpectation> expectations;

    private HttpServiceExpectation currentExpectation;

    public IntegrationTestExpectations(BarWriter bar, int port) {
        this.bar = bar;
        this.expectations = new ArrayList<>();
        this.server = new HttpServer(port, this::handleRequest);
    }

    @Override public URI baseUri() {return server.baseUri();}

    @Override public Object invoke(Method method, Object... args) {
        for (var expectation : expectations) {
            if (expectation.matches(method, args)) {
                this.currentExpectation = expectation;
                try {
                    return expectation.invoke();
                } finally {
                    this.currentExpectation = null;
                }
            }
        }

        var expectation = createFor(method, args);
        expectations.add(expectation);
        WunderbarExpectationBuilder.buildingExpectation = expectation;

        return expectation.nullValue();
    }

    private HttpServiceExpectation createFor(Method method, Object... args) {
        var declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(GraphQLClientApi.class))
            return new GraphQlExpectation(server, method, args);
        if (declaringClass.isAnnotationPresent(Path.class))
            return new RestExpectation(server, method, args);
        throw new WunderBarException("no technology recognized on " + declaringClass);
    }

    private HttpResponse handleRequest(HttpRequest request) {
        if (request.getBody().isPresent() && isCompatible(APPLICATION_JSON_TYPE, request.getContentType()))
            request = request.withBody(Optional.of(formatJson(request.getBody().get())));

        if (currentExpectation == null)
            return HttpResponse.builder().status(NOT_IMPLEMENTED).body("no current expectation set").build();

        var response = currentExpectation.handleRequest(request);

        if (bar != null) bar.save(request, response);

        return response;
    }

    @Override public void done() {
        expectations.forEach(WunderBarExpectation::done);
        expectations.clear();
    }
}
