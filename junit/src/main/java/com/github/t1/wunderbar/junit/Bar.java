package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class Bar {
    static Bar bar;

    public static Function<HttpServerRequest, HttpServerResponse> save(String name, Function<HttpServerRequest, HttpServerResponse> handler) {
        return (request) -> {
            bar.save(name, request);
            var response = handler.apply(request);
            bar.save(name, response);
            return response;
        };
    }

    private void save(String name, HttpServerRequest request) {
        log.debug("request {}:\n{}", name, request);
    }

    private void save(String name, HttpServerResponse response) {
        log.debug("response {}:\n{}", name, response);
    }
}
