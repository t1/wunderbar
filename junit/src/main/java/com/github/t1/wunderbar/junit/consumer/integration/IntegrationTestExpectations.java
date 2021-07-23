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
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.Utils.formatJson;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Slf4j
public class IntegrationTestExpectations implements WunderBarExpectations {
    private final BarWriter bar;

    @Getter private final HttpServer server;
    private final List<HttpServiceExpectation> expectations;

    private Function<HttpRequest, HttpResponse> currentHandler;

    public IntegrationTestExpectations(BarWriter bar, int port) {
        this.bar = bar;
        this.expectations = new ArrayList<>();
        this.server = new HttpServer(port, httpRequest -> currentHandler.apply(httpRequest));
    }

    @Override public URI baseUri() { return server.baseUri(); }

    @Override public Object invoke(Method method, Object... args) {
        for (var expectation : expectations) {
            if (expectation.matches(method, args)) {
                this.currentHandler = wrapHandler(expectation);
                try {
                    return expectation.invoke();
                } finally {
                    this.currentHandler = null;
                }
            }
        }

        var expectation = createFor(method, args);
        expectations.add(expectation);
        WunderbarExpectationBuilder.buildingExpectation = expectation;

        return expectation.nullValue();
    }

    private Function<HttpRequest, HttpResponse> wrapHandler(HttpServiceExpectation expectation) {
        Function<HttpRequest, HttpResponse> handler = expectation::handleRequest;
        if (bar != null) handler = save(bar, handler);
        handler = formatRequestBody(handler);
        return handler;
    }

    private Function<HttpRequest, HttpResponse> save(BarWriter bar, Function<HttpRequest, HttpResponse> handler) {
        return request -> {
            var response = handler.apply(request);
            bar.save(request, response);
            return response;
        };
    }

    private Function<HttpRequest, HttpResponse> formatRequestBody(Function<HttpRequest, HttpResponse> handler) {
        return request -> {
            if (request.getBody().isPresent() && APPLICATION_JSON_TYPE.isCompatible(request.getContentType()))
                request = request.withBody(Optional.of(formatJson(request.getBody().get())));
            return handler.apply(request);
        };
    }

    private HttpServiceExpectation createFor(Method method, Object... args) {
        var declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(GraphQLClientApi.class))
            return new GraphQlExpectation(server, method, args);
        if (declaringClass.isAnnotationPresent(RegisterRestClient.class))
            return new RestExpectation(server, method, args);
        throw new WunderBarException("no technology recognized on " + declaringClass);
    }

    @Override public void done() {
        expectations.forEach(WunderBarExpectation::done);
        expectations.clear();
    }
}
