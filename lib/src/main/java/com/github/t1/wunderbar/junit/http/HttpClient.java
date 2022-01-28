package com.github.t1.wunderbar.junit.http;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;

import static com.github.t1.wunderbar.junit.http.HttpUtils.charset;
import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;

@RequiredArgsConstructor
public class HttpClient {
    private static final java.net.http.HttpClient HTTP = java.net.http.HttpClient.newBuilder()
        .followRedirects(NORMAL)
        .connectTimeout(Duration.ofSeconds(1))
        .build();

    private final URI baseUri;

    @SneakyThrows({IOException.class, InterruptedException.class})
    public HttpResponse send(HttpRequest request) {
        var httpRequest = convert(request);

        var httpResponse = HTTP.send(httpRequest, bodyHandler());

        return convert(httpResponse);
    }

    private java.net.http.HttpRequest convert(HttpRequest request) {
        var builder = java.net.http.HttpRequest.newBuilder()
            // using URI#resolve would remove the context path of the base uri
            .uri(URI.create(baseUri + "" + request.getUri()))
            .method(request.getMethod(), request.body().map(BodyPublishers::ofString).orElse(noBody()))
            .header(CONTENT_TYPE, request.getContentType().toString())
            .header(ACCEPT, request.getAccept().toString());
        if (request.getAuthorization() != null) builder.header(AUTHORIZATION, request.getAuthorization().toHeader());
        return builder.build();
    }

    private BodyHandler<String> bodyHandler() {
        return (responseInfo) -> BodySubscribers.ofString(
            charset(responseInfo.headers().firstValue(CONTENT_TYPE).map(MediaType::valueOf).orElse(null)));
    }

    private HttpResponse convert(java.net.http.HttpResponse<String> response) {
        return HttpResponse.builder()
            .status(Status.fromStatusCode(response.statusCode()))
            .contentType(contentType(response))
            .body(response.body())
            .build();
    }

    private MediaType contentType(java.net.http.HttpResponse<?> response) {
        return response.headers().firstValue(CONTENT_TYPE).map(MediaType::valueOf).orElse(WILDCARD_TYPE);
    }
}
