package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import com.github.t1.wunderbar.junit.http.HttpUtils;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.BDDSoftAssertions;
import org.junit.jupiter.api.function.Executable;

import javax.json.Json;
import javax.json.JsonPointer;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.Utils.formatJson;
import static com.github.t1.wunderbar.junit.http.HttpUtils.charset;
import static com.github.t1.wunderbar.junit.provider.CustomBDDAssertions.then;
import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpResponse.BodySubscribers.mapping;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@RequiredArgsConstructor
class HttpBarExecutable implements Executable {

    public static HttpBarExecutable of(BarReader bar, Test test) { return new HttpBarExecutable(bar, test); }

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .followRedirects(NORMAL)
        .connectTimeout(Duration.ofSeconds(1))
        .build();

    private final BarReader bar;
    private final Test test;

    private final WunderBarApiProviderJUnitExtension extension = WunderBarApiProviderJUnitExtension.INSTANCE;

    @Override public void execute() throws IOException, InterruptedException {
        var interactions = bar.interactionsFor(test);

        System.out.println("==================== start " + test);
        extension.beforeDynamicTestMethods.forEach(consumer -> consumer.accept(interactions));

        for (var interaction : interactions)
            new Execution(interaction).run();

        extension.afterDynamicTestMethods.forEach(consumer -> consumer.accept(interactions));
    }

    @AllArgsConstructor
    private class Execution {
        HttpServerInteraction interaction;

        public void run() throws IOException, InterruptedException {
            extension.beforeInteractionMethods.forEach(this::apply);
            System.out.println("request " + interaction.getNumber() + ":\n" + interaction.getRequest() + "\n");

            HttpResponse<JsonValue> actual = HTTP.send(request(interaction.getRequest()), ofJson());

            System.out.println("received: " + actual);
            var expected = interaction.getResponse();
            extension.afterInteractionMethods.forEach(consumer -> consumer.apply(interaction));
            thenSoftly(softly -> checkResponse(softly, actual, expected));
        }

        private void apply(Function<HttpServerInteraction, Object> consumer) {
            var result = consumer.apply(interaction);
            if (result instanceof HttpServerInteraction) interaction = (HttpServerInteraction) result;
            if (result instanceof HttpServerRequest) interaction = interaction.withRequest((HttpServerRequest) result);
            if (result instanceof HttpServerResponse) interaction = interaction.withResponse((HttpServerResponse) result);
        }

        private BodyHandler<JsonValue> ofJson() {
            return (responseInfo) -> {
                var charset = charset(responseInfo.headers().firstValue(CONTENT_TYPE).map(MediaType::valueOf).orElse(null));
                return mapping(BodySubscribers.ofString(charset), HttpUtils::toJson);
            };
        }

        private HttpRequest request(HttpServerRequest request) {
            var builder = HttpRequest.newBuilder()
                // using URI#resolve would remove the context path of the base uri
                .uri(URI.create(extension.baseUri() + "" + request.getUri()))
                .method(request.getMethod(), request.getBody().map(BodyPublishers::ofString).orElse(noBody()))
                .header(CONTENT_TYPE, request.getContentType().toString())
                .header(ACCEPT, request.getAccept().toString());
            if (request.getAuthorization() != null) builder.header(AUTHORIZATION, request.getAuthorization().toHeader());
            return builder.build();
        }

        private void checkResponse(BDDSoftAssertions softly, HttpResponse<JsonValue> actual, HttpServerResponse expected) {
            softly.then(HttpUtils.toString(Status.fromStatusCode(actual.statusCode()))).as("status")
                .isEqualTo(HttpUtils.toString(expected.getStatus()));
            var actualContentType = contentType(actual);
            softly.then(actualContentType.isCompatible(expected.getContentType()))
                .describedAs("Content-Type: " + expected.getContentType() + " to be compatible to " + actualContentType)
                .isTrue();
            expected.getBody().map(HttpUtils::toJson).ifPresent(expectedBody -> checkBody(softly, actual.body(), expectedBody));
        }

        private MediaType contentType(HttpResponse<?> response) {
            return response.headers().firstValue(CONTENT_TYPE).map(MediaType::valueOf).orElse(WILDCARD_TYPE);
        }

        private void checkBody(BDDSoftAssertions softly, JsonValue actual, JsonValue expected) {
            System.out.println(formatJson(actual));
            if (actual instanceof JsonStructure && expected instanceof JsonStructure)
                checkForUnexpectedErrors(softly, (JsonStructure) actual, (JsonStructure) expected);
            softly.check(() -> then(actual).isEqualToIgnoringNewFields(expected));
        }

        /** GraphQL: show unexpected errors in addition to the missing or only partial data */
        private void checkForUnexpectedErrors(BDDSoftAssertions softly, JsonStructure actual, JsonStructure expected) {
            if (ERRORS.containsValue(actual) && !ERRORS.containsValue(expected))
                softly.then(ERRORS.getValue(actual)).as("errors").isNull();
        }
    }

    private static final JsonPointer ERRORS = Json.createPointer("/errors");
}
