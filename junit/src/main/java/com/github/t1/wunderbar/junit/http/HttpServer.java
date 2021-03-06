package com.github.t1.wunderbar.junit.http;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.http.HttpUtils.charset;
import static io.undertow.util.Headers.ACCEPT;
import static io.undertow.util.Headers.AUTHORIZATION;
import static io.undertow.util.Headers.CONTENT_TYPE;

public class HttpServer {
    private final Function<HttpServerRequest, HttpServerResponse> handler;
    private final Undertow server;

    public HttpServer(@NonNull Function<HttpServerRequest, HttpServerResponse> handler) {
        this.handler = handler;
        this.server = start();
    }

    private Undertow start() {
        Undertow server = Undertow.builder()
            .addHttpListener(0, "localhost")
            .setHandler(this::aroundInvoke)
            .build();
        server.start();
        return server;
    }

    private void aroundInvoke(HttpServerExchange exchange) {
        var requestBuilder = HttpServerRequest.builder()
            .method(exchange.getRequestMethod().toString())
            .uri(URI.create(exchange.getRequestURI()))
            .contentType(HttpUtils.firstMediaType(exchange.getRequestHeaders().getFirst(CONTENT_TYPE)))
            .accept(HttpUtils.firstMediaType(exchange.getRequestHeaders().getFirst(ACCEPT)))
            .authorization(Authorization.valueOf(exchange.getRequestHeaders().getFirst(AUTHORIZATION)));
        readRequestBody(exchange).ifPresent(requestBuilder::body);
        var request = requestBuilder.build();

        var response = handler.apply(request);

        exchange.setStatusCode(response.getStatus().getStatusCode());
        exchange.getResponseHeaders().put(CONTENT_TYPE, response.getContentType().toString());
        response.getBody().ifPresent(body ->
            exchange.getResponseSender().send(body, charset(response.getContentType())));
    }

    private Optional<String> readRequestBody(HttpServerExchange httpServerExchange) {
        if (httpServerExchange.isRequestComplete()) return Optional.empty();
        var body = new AtomicReference<String>();
        httpServerExchange.getRequestReceiver().receiveFullString((x, value) -> body.setRelease(value),
            Charset.forName(httpServerExchange.getRequestCharset()));
        return Optional.ofNullable(body.getAcquire());
    }

    public URI baseUri() {
        var listener = server.getListenerInfo().get(0);
        var address = (InetSocketAddress) listener.getAddress();
        return URI.create(listener.getProtcol() + "://" + address.getHostString() + ":" + address.getPort());
    }

    public void stop() {
        server.stop();
    }
}
