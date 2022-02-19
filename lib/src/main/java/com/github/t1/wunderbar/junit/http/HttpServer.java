package com.github.t1.wunderbar.junit.http;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.http.HttpUtils.charset;
import static io.undertow.util.Headers.CONTENT_TYPE;

@Slf4j
public class HttpServer {
    private final Function<HttpRequest, HttpResponse> handler;
    private final Undertow server;
    private final int port;

    public HttpServer(@NonNull Function<HttpRequest, HttpResponse> handler) {
        this(0, handler);
    }

    public HttpServer(int port, @NonNull Function<HttpRequest, HttpResponse> handler) {
        this.handler = handler;
        this.server = start(port);
        this.port = port();
        log.debug("server started on port " + baseUri().getPort());
    }

    private Undertow start(int port) {
        Undertow server = Undertow.builder()
            .addHttpListener(port, "localhost")
            .setHandler(this::aroundInvoke)
            .build();
        server.start();
        return server;
    }

    private int port() {
        var listener = server.getListenerInfo().get(0);
        var address = (InetSocketAddress) listener.getAddress();
        return address.getPort();
    }

    private void aroundInvoke(HttpServerExchange exchange) {
        HttpRequest request = buildRequest(exchange);

        var response = handler.apply(request);

        exchange.setStatusCode(response.getStatusCode());
        exchange.getResponseHeaders().put(CONTENT_TYPE, response.getContentType().toString());
        response.body().ifPresent(body ->
            exchange.getResponseSender().send(body, charset(response.getContentType())));
    }

    private HttpRequest buildRequest(HttpServerExchange exchange) {
        var requestBuilder = HttpRequest.builder()
            .method(exchange.getRequestMethod().toString())
            .uri(URI.create(exchange.getRequestURI()));

        exchange.getRequestHeaders().forEach(headerValue ->
            requestBuilder.header(headerValue.getHeaderName().toString(), List.of(headerValue.toArray())));

        readRequestBody(exchange).ifPresent(requestBuilder::body);

        return requestBuilder.build();
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

    public URI baseUri() {return URI.create("http://localhost:" + port);}

    public void stop() {
        server.stop();
        log.debug("server stopped on port " + port);
    }
}
