package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;
import com.github.t1.wunderbar.junit.OpenApiContract;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.writeString;

class OpenApiBarWriter extends BarWriter {
    @Getter private final Path path;
    @Setter private String comment;
    @Getter @Setter private String directory;
    private final Map<String, List<HttpInteraction>> tests = new LinkedHashMap<>();

    @SneakyThrows(IOException.class)
    OpenApiBarWriter(Path path) {
        this.path = path;
        Files.deleteIfExists(path);
    }

    @Override public AtomicInteger counter() {return new AtomicInteger(currentInteractions().size());}

    @Override public void save(HttpRequest request, HttpResponse response) {
        currentInteractions().add(new HttpInteraction(currentInteractions().size() + 1, request, response));
        persist();
    }

    private List<HttpInteraction> currentInteractions() {
        if (directory == null)
            throw new IllegalStateException("must set directory before calling save: " + this);
        return tests.computeIfAbsent(directory, __ -> new ArrayList<>());
    }

    @Override protected void write(String fileName, String content) {
        throw new UnsupportedOperationException("OpenAPI writer does not write BAR entries");
    }

    @Override public void close() {persist();}

    @SneakyThrows(IOException.class)
    private void persist() {
        if (tests.isEmpty()) return;
        var parent = path.getParent();
        if (parent != null) createDirectories(parent);
        writeString(path, OpenApiContract.toJson(testCases(), OpenApiContract.versionFromComment(comment)));
    }

    private List<OpenApiContract.TestCase> testCases() {
        return tests.entrySet().stream()
                .map(entry -> new OpenApiContract.TestCase(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }
}
