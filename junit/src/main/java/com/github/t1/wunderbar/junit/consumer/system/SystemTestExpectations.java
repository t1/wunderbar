package com.github.t1.wunderbar.junit.consumer.system;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.Depletion;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder;
import com.github.t1.wunderbar.junit.consumer.integration.HttpServiceExpectation;
import com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client.JaxRsTypesafeGraphQLClientBuilder;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

@Slf4j
public class SystemTestExpectations<T> implements WunderBarExpectations<T> {
    /*
     * Implementation notes:
     * - SUT: inject not the mock but the real service (this is also the case, if the SUT runs remotely)
     * - the stub call is in two steps:
     *   1. call a stub server (the same as in the integration tests) in order to record request&response
     *   2. push that as an expectation into the mock server
     * - remove all expectations from the mock server
     */
    private final Technology technology;
    private final Class<T> type;
    private final URI baseUri;
    private final BarFilter filter;
    private final T api;
    private final WunderBarMockServerApi mock;
    private final HttpServer stubServer;
    private boolean didAddExpectations = false;
    private HttpServiceExpectation currentExpectation;
    private HttpInteraction currentInteraction;

    public SystemTestExpectations(Class<T> type, URI baseUri, Technology technology, BarWriter bar) {
        this.technology = technology;
        this.type = type;
        this.baseUri = baseUri;
        this.filter = new BarFilter(bar, this::isRecording);
        this.api = buildApi();
        this.mock = new JaxRsTypesafeGraphQLClientBuilder().endpoint(mockEndpoint(baseUri)).build(WunderBarMockServerApi.class);
        this.stubServer = new HttpServer(this::handleStubRequest);
    }

    private URI mockEndpoint(URI baseUri) {
        if (baseUri.getPath().endsWith("/rest")) {
            var uri = baseUri.toString();
            return URI.create(uri.substring(0, uri.length() - 5) + "/graphql");
        }
        return baseUri;
    }

    private T buildApi() {
        log.info("build {} system test endpoint: {}", technology, baseUri);
        return switch (technology) {
            case GRAPHQL -> new JaxRsTypesafeGraphQLClientBuilder()
                    .register(filter)
                    .endpoint(baseUri)
                    .build(type);
            case REST -> RestClientBuilder.newBuilder()
                    .register(filter)
                    .baseUri(baseUri)
                    .build(type);
        };
    }

    @Override public URI baseUri() {return baseUri;}

    @Override public T asSutProxy(T proxy) {return api;}

    @Override public Object invoke(Method method, Object... args) {
        // as the SUT only knows about the real service, these invocations can only be from stubbing
        log.debug("---------- start building expectation for {}", method.getName());
        var stubUri = stubServer.baseUri().resolve(baseUri.getPath());
        this.currentExpectation = HttpServiceExpectation.of(technology, stubUri, method, args)
            .afterStubbing(this::addExpectationToMockService);
        WunderbarExpectationBuilder.buildingExpectation = currentExpectation;
        return currentExpectation.nullValue();
    }

    private void addExpectationToMockService() {
        buildExpectation();
        addExpectation();
    }

    private void buildExpectation() {
        log.debug("call stub service");
        try {
            currentExpectation.invoke();
            log.debug("---------- stub service returned");
        } catch (RuntimeException e) {
            if (currentExpectation.hasException()) log.debug("---------- ignore expected exception from stub service: {}", e.toString());
            else throw e;
        }
    }

    private HttpResponse handleStubRequest(HttpRequest request) {
        request = request.normalized();
        var response = currentExpectation.handleRequest(request);
        this.currentInteraction = new HttpInteraction(0, request, response);
        return response;
    }

    private void addExpectation() {
        log.debug("add expectation to mock service");
        var stubbingResult = addWunderBarExpectation();
        log.debug("---------- add expectation and stubbing done -> {}", stubbingResult);
        if (stubbingResult == null || !"ok".equals(stubbingResult.getStatus()))
            throw new WunderBarException("unexpected response from adding expectation to mock server: " + stubbingResult);
        this.didAddExpectations = true;
    }

    private WunderBarStubbingResult addWunderBarExpectation() {
        if (currentInteraction == null) throw new IllegalStateException("the stub call didn't get through");
        try {
            return mock.addWunderBarExpectation(
                currentInteraction.getRequest().normalized().withoutContextPath(),
                currentExpectation.getDepletion(),
                currentInteraction.getResponse());
        } catch (Exception e) {
            throw new WunderBarException("failed to add expectation to mock server; maybe it's a real service", e);
        }
    }

    @SneakyThrows(IOException.class)
    @Override public void done() {
        log.debug("---------- call done -- cleanup");
        if (didAddExpectations) {
            var status = mock.cleanupWunderBarExpectations();
            assert "ok".equals(status);
        }
        if (api instanceof Closeable) ((Closeable) api).close();
        log.debug("---------- cleanup done");
        stubServer.stop();
    }

    private Boolean isRecording() {
        return currentExpectation == null || // i.e. this is a call to the SUT, so you can't say `withoutRecording`
               currentExpectation.isRecording();
    }

    @GraphQLClientApi
    private interface WunderBarMockServerApi {
        @Mutation WunderBarStubbingResult addWunderBarExpectation(
            @NonNull HttpRequest request,
            @NonNull Depletion depletion,
            @NonNull HttpResponse response);
        @Mutation String cleanupWunderBarExpectations();
    }

    @Data
    private static class WunderBarStubbingResult {
        int id;
        String status;
    }
}
