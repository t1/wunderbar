package com.github.t1.wunderbar.junit.runner;

import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import com.github.t1.wunderbar.junit.http.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.function.Executable;

import javax.json.JsonValue;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;

import static com.github.t1.wunderbar.junit.http.HttpUtils.charset;
import static com.github.t1.wunderbar.junit.runner.CustomBDDAssertions.then;
import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpResponse.BodySubscribers.mapping;
import static java.net.http.HttpResponse.ResponseInfo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@Slf4j
@RequiredArgsConstructor
class BarExecutor implements Executable {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .followRedirects(NORMAL)
        .connectTimeout(Duration.ofSeconds(1))
        .build();

    private final URI baseUri;
    private final HttpServerInteraction interaction;

    @Override public void execute() throws IOException, InterruptedException {
        var interactions = List.of(this.interaction); // TODO before/after for all invocations in one test
        WunderBarRunnerJUnitExtension.INSTANCE.beforeBarTestConsumers.forEach(consumer -> consumer.accept(interactions));

        log.debug("send {}", this.interaction.getRequest());

        HttpResponse<JsonValue> actual = HTTP.send(request(), ofJson());

        WunderBarRunnerJUnitExtension.INSTANCE.afterBarTestConsumers.forEach(consumer -> consumer.accept(interactions));

        log.debug("received {}", actual);
        var expected = this.interaction.getResponse();
        thenSoftly(softly -> {
            softly.then(actual.statusCode()).isEqualTo(expected.getStatus().getStatusCode());
            softly.then(contentType(actual).isCompatible(expected.getContentType()))
                .describedAs(expected.getContentType() + " to be compatible to " + contentType(actual))
                .isTrue();
            expected.getBody().map(HttpUtils::toJson).ifPresent(expectedBody ->
                softly.check(() -> then(actual.body()).isEqualToIgnoringNewFields(expectedBody)));
        });
    }

    private HttpRequest request() {
        var request = interaction.getRequest();
        return HttpRequest.newBuilder()
            .uri(baseUri.resolve(request.getUri()))
            .method(request.getMethod(), request.getBody().map(BodyPublishers::ofString).orElse(noBody()))
            .build();
    }

    private BodyHandler<JsonValue> ofJson() {
        return (responseInfo) -> mapping(BodySubscribers.ofString(getCharset(responseInfo)), HttpUtils::toJson);
    }

    private Charset getCharset(ResponseInfo responseInfo) {
        return charset(responseInfo.headers().firstValue(CONTENT_TYPE).map(MediaType::valueOf).orElse(null));
    }

    private MediaType contentType(HttpResponse<?> response) {
        return response.headers().firstValue(CONTENT_TYPE)
            .map(MediaType::valueOf)
            .orElseThrow(() -> new AssertionError("expected a " + CONTENT_TYPE + " header"));
    }
}
