package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.t1.wunderbar.junit.http.HttpUtils.formatJson;
import static com.github.t1.wunderbar.junit.http.HttpUtils.isCompatible;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;

@Slf4j
public @Internal class IntegrationTestExpectations implements WunderBarExpectations {
    private final BarWriter bar;
    private final Technology technology;
    private final List<HttpServiceExpectation> expectations = new ArrayList<>();
    @Getter private final HttpServer server;

    private HttpServiceExpectation currentExpectation;

    public IntegrationTestExpectations(URI endpoint, Technology technology, BarWriter bar) {
        this.server = new HttpServer(endpoint.getPort(), this::handleRequest);
        this.technology = technology;
        this.bar = bar;
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

        var expectation = HttpServiceExpectation.of(technology, server, method, args);
        expectations.add(expectation);
        WunderbarExpectationBuilder.buildingExpectation = expectation;

        return expectation.nullValue();
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
