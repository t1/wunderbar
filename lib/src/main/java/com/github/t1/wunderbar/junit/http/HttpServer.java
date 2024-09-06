package com.github.t1.wunderbar.junit.http;

import com.sun.net.httpserver.HttpExchange;
import jakarta.ws.rs.core.MediaType;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Function;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.CHARSET_PARAMETER;
import static jakarta.ws.rs.core.MediaType.WILDCARD_TYPE;
import static jakarta.ws.rs.core.MediaType.valueOf;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class HttpServer {
    private static final int DEFAULT_CONTENT_LENGTH = 64 * 1024;

    private final Function<HttpRequest, HttpResponse> handler;
    private final com.sun.net.httpserver.HttpServer server;

    public HttpServer(@NonNull Function<HttpRequest, HttpResponse> handler) {
        this(0, handler);
    }

    public HttpServer(int port, @NonNull Function<HttpRequest, HttpResponse> handler) {
        this.handler = handler;
        this.server = start(port);
        log.debug("server started on port {}", baseUri().getPort());
    }

    @SneakyThrows(IOException.class)
    private com.sun.net.httpserver.HttpServer start(int port) {
        var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.start();
        return server;
    }

    private void handle(HttpExchange exchange) {
        var request = buildRequest(exchange);

        HttpResponse response;
        try {
            response = handler.apply(request);
        } catch (RuntimeException e) {
            response = failure(request, e);
        }

        write(exchange, response);
    }

    private HttpResponse failure(HttpRequest request, RuntimeException e) {
        log.error("failed to handle request {}", request, e);
        return HttpResponse.builder().status(INTERNAL_SERVER_ERROR).build();
    }

    private HttpRequest buildRequest(HttpExchange exchange) {
        var requestBuilder = HttpRequest.builder()
                .method(exchange.getRequestMethod())
                .uri(exchange.getRequestURI());

        exchange.getRequestHeaders().forEach(requestBuilder::header);
        requestBuilder.contentType(contentType(exchange));

        readRequestBody(exchange).ifPresent(requestBuilder::body);

        return requestBuilder.build();
    }

    @SneakyThrows(IOException.class)
    private Optional<String> readRequestBody(HttpExchange exchange) {
        try (var inputStream = exchange.getRequestBody()) {
            var contentLength = contentLength(exchange);
            if (contentLength.isPresent() && contentLength.get() == 0) return Optional.empty();
            var buffer = new byte[contentLength.orElse(DEFAULT_CONTENT_LENGTH)];
            var read = inputStream.read(buffer);
            return read == -1 ? Optional.empty() : Optional.of(new String(buffer, charset(exchange)));
        }
    }

    private static Optional<Integer> contentLength(HttpExchange exchange) {
        var contentLength = exchange.getRequestHeaders().getFirst(CONTENT_LENGTH);
        return contentLength == null ? Optional.empty() : Optional.of(Integer.parseInt(contentLength));
    }

    private Charset charset(HttpExchange exchange) {
        var charset = contentType(exchange).getParameters().get(CHARSET_PARAMETER);
        return charset == null ? UTF_8 : Charset.forName(charset);
    }

    private MediaType contentType(HttpExchange exchange) {
        var contentType = exchange.getRequestHeaders().getFirst(CONTENT_TYPE);
        return (contentType == null) ? WILDCARD_TYPE : valueOf(contentType);
    }

    @SneakyThrows(IOException.class)
    private void write(HttpExchange exchange, HttpResponse response) {
        exchange.getResponseHeaders().add("Server", "Wunderbar");
        exchange.getResponseHeaders().add(CONTENT_TYPE, response.getContentType().toString());
        var bytes = response.bytes();
        exchange.sendResponseHeaders(response.getStatusCode(), bytes == null ? 0 : bytes.length);
        if (bytes != null) exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public URI baseUri() {return URI.create("http://localhost:" + server.getAddress().getPort());}

    public void stop() {
        server.stop(0);
        log.debug("server stopped");
    }
}
