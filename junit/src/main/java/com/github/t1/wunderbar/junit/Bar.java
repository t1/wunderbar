package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final String name;
    @Getter @Setter Path path = Paths.get("target/wunder.bar");
    @Setter private String directory;
    private JarOutputStream archive;
    private final Map<String, AtomicInteger> counters = new LinkedHashMap<>();

    public Function<HttpServerRequest, HttpServerResponse> save(Function<HttpServerRequest, HttpServerResponse> handler) {
        return (request) -> {
            var prefix = nextPrefix();
            save(prefix, request);

            var response = handler.apply(request);

            save(prefix, response);
            return response;
        };
    }

    private String nextPrefix() {
        assert directory != null : "must set directory before calling save";
        var counter = counters.computeIfAbsent(directory, i -> new AtomicInteger())
            .incrementAndGet();
        return directory + "/" + counter + " ";
    }


    private void save(String id, HttpServerRequest request) {
        write(id + "request-headers.properties", request.headerProperties());
        request.getBody().ifPresent(body -> write(id + "request-body.json", body));
    }

    private void save(String id, HttpServerResponse response) {
        write(id + "response-headers.properties", response.headerProperties());
        response.getBody().ifPresent(body -> write(id + "response-body.json", body));
    }

    @SneakyThrows(IOException.class)
    private void write(String fileName, String content) {
        var archive = archive();
        archive.putNextEntry(new ZipEntry(fileName));
        var bytes = content.getBytes(UTF_8);
        log.debug("write {} bytes to {}", bytes.length, fileName);
        archive.write(bytes);
        archive.closeEntry();
    }

    @SneakyThrows(IOException.class)
    private JarOutputStream archive() {
        if (archive == null) {
            Files.deleteIfExists(path);
            var outputStream = Files.newOutputStream(path);
            archive = new JarOutputStream(outputStream);
            archive.setComment(name);
        }
        return archive;
    }

    @SneakyThrows(IOException.class)
    public void close() { if (archive != null) archive.close(); }
}
