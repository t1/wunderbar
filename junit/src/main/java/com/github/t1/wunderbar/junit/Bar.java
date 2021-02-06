package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j @RequiredArgsConstructor
public class Bar {
    static Bar bar;

    public static Function<HttpServerRequest, HttpServerResponse> save(String id, Function<HttpServerRequest, HttpServerResponse> handler) {
        return (request) -> {
            var prefix = bar.prefix(id);
            bar.save(prefix, request);
            var response = handler.apply(request);
            bar.save(prefix, response);
            return response;
        };
    }

    private final String name;
    private JarOutputStream archive;
    private final Map<String, AtomicInteger> counters = new LinkedHashMap<>();

    private String prefix(String id) {
        var counter = bar.counters.computeIfAbsent(id, i -> new AtomicInteger()).incrementAndGet();
        return id + "/" + counter + " ";
    }

    @SneakyThrows(IOException.class)
    private JarOutputStream archive() {
        if (archive == null) {
            var path = Paths.get("target/" + name + ".bar");
            Files.deleteIfExists(path);
            var outputStream = Files.newOutputStream(path);
            archive = new JarOutputStream(outputStream);
        }
        return archive;
    }


    private void save(String id, HttpServerRequest request) {
        log.debug("request {}:\n{}", id, request);
        write(id + "request-headers.properties", headers(request));
        request.getBody().ifPresent(body -> write(id + "request-body.json", body));
    }

    private String headers(HttpServerRequest request) {
        return "" +
            "Method: " + request.getMethod() + "\n" +
            "URI: " + request.getUri() + "\n" +
            "Content-Type: " + request.getContentType() + "\n" +
            "Accept: " + request.getAccept() + "\n" +
            "";
    }


    private void save(String id, HttpServerResponse response) {
        log.debug("response {}:\n{}", id, response);
        write(id + "response-headers.properties", headers(response));
        response.getBody().ifPresent(body -> write(id + "response-body.json", body));
    }

    private String headers(HttpServerResponse response) {
        return "" +
            "Status: " + response.getStatus() + "\n" +
            "Content-Type: " + response.getContentType() + "\n" +
            "";
    }


    @SneakyThrows(IOException.class)
    private void write(String fileName, String content) {
        archive().putNextEntry(new ZipEntry(fileName));
        var bytes = content.getBytes(UTF_8);
        log.debug("write {} bytes to {}", bytes.length, fileName);
        archive().write(bytes);
        archive().closeEntry();
    }

    @SneakyThrows(IOException.class)
    public void close() { if (archive != null) archive.close(); }
}
