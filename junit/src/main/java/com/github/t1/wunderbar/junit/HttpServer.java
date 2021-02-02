package com.github.t1.wunderbar.junit;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URI;

@Slf4j
class HttpServer {
    private final Undertow server;

    HttpServer(HttpHandler handler) {
        this.server = Undertow.builder()
            .addHttpListener(0, "localhost")
            .setHandler(handler)
            .build();
        server.start();
    }

    URI baseUri() {
        var listener = server.getListenerInfo().get(0);
        var address = (InetSocketAddress) listener.getAddress();
        var uri = URI.create(listener.getProtcol() + "://" + address.getHostString() + ":" + address.getPort() + "/mock");
        log.debug("baseUri: {}", uri);
        return uri;
    }

    void stop() { server.stop(); }
}
