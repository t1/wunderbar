package com.github.t1.wunderbar.junit.http;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import lombok.NonNull;

import javax.ws.rs.core.MediaType;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.undertow.util.Headers.ACCEPT;
import static io.undertow.util.Headers.CONTENT_TYPE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static javax.ws.rs.core.MediaType.CHARSET_PARAMETER;

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
        var request = HttpServerRequest.builder()
            .method(exchange.getRequestMethod().toString())
            .uri(URI.create(exchange.getRequestURI()))
            .contentType(mediaType(exchange.getRequestHeaders().getFirst(CONTENT_TYPE)))
            .accept(mediaType(exchange.getRequestHeaders().getFirst(ACCEPT)))
            .body(readRequestBody(exchange))
            .build();

        var response = handler.apply(request);

        exchange.setStatusCode(response.getStatus().getStatusCode());
        exchange.getResponseHeaders().put(CONTENT_TYPE, response.getContentType().toString());
        response.getBody().ifPresent(body ->
            exchange.getResponseSender().send(body, charset(response.getContentType())));
    }

    private MediaType mediaType(String string) {
        return (string == null) ? null : MediaType.valueOf(string);
    }

    private Optional<String> readRequestBody(HttpServerExchange httpServerExchange) {
        var body = new AtomicReference<String>();
        httpServerExchange.getRequestReceiver().receiveFullString((x, value) -> body.setRelease(value),
            Charset.forName(httpServerExchange.getRequestCharset()));
        return Optional.of(body.getAcquire());
    }

    private Charset charset(MediaType contentType) {
        var charsetName = contentType.getParameters().get(CHARSET_PARAMETER);
        return (charsetName == null) ? ISO_8859_1 : Charset.forName(charsetName);
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
