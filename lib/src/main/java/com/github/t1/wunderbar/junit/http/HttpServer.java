package com.github.t1.wunderbar.junit.http;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import lombok.NonNull;
import lombok.SneakyThrows;

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
    private final Function<HttpRequest, HttpResponse> handler;
    private final Undertow server;

    public HttpServer(@NonNull Function<HttpRequest, HttpResponse> handler) {
        this(0, handler);
    }

    public HttpServer(int port, @NonNull Function<HttpRequest, HttpResponse> handler) {
        this.handler = handler;
        this.server = start(port);
    }

    private Undertow start(int port) {
        Undertow server = Undertow.builder()
            .addHttpListener(port, LOCALHOST)
            .setHandler(this::aroundInvoke)
            .build();
        server.start();
        return server;
    }

    private void aroundInvoke(HttpServerExchange exchange) {
        var requestBuilder = HttpRequest.builder()
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

    @SneakyThrows(InterruptedException.class)
    private Optional<String> readRequestBody(HttpServerExchange httpServerExchange) {
        if (httpServerExchange.isRequestComplete()) return Optional.empty();
        var body = new AtomicReference<String>();
        Thread.sleep(1); // it's strange, but without this, Undertow sometimes looses the body
        var charset = Charset.forName(httpServerExchange.getRequestCharset());
        httpServerExchange.getRequestReceiver().receiveFullString((exchange, value) -> body.setRelease(value), charset);
        return Optional.ofNullable(body.getAcquire());
    }

    public URI baseUri() {
        var listener = server.getListenerInfo().get(0);
        var address = (InetSocketAddress) listener.getAddress();
        return URI.create(listener.getProtcol() + "://" + LOCALHOST + ":" + address.getPort());
    }

    public void stop() {
        server.stop();
    }

    private static final String LOCALHOST = "localhost";
}
