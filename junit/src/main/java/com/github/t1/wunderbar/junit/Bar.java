package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class Bar {
    static Bar bar;

    public static Function<HttpServerRequest, HttpServerResponse> save(
        Supplier<String> nameSupplier,
        Function<HttpServerRequest, HttpServerResponse> handler) {
        return (request) -> {
            var name = nameSupplier.get();
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
