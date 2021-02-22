package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;

import java.io.Closeable;
import java.nio.file.Path;

public abstract class BarWriter implements Closeable {
    public static BarWriter of(String fileName) {
        Path path = Path.of(fileName);
        return fileName.endsWith("/") ? new DirectoryBarWriter(path) : new JarBarWriter(path);
    }

    public abstract Path getPath();

    public abstract void setComment(String directory);

    public abstract void setDirectory(String directory);

    public abstract String getDirectory();

    public final void save(HttpServerRequest request, HttpServerResponse response) {
        String id = getDirectory() + "/" + (count() + 1) + " ";
        write(id + "request-headers.properties", request.headerProperties());
        request.getBody().ifPresent(body -> write(id + "request-body.json", body));

        write(id + "response-headers.properties", response.headerProperties());
        response.getBody().ifPresent(body -> write(id + "response-body.json", body));
    }

    protected abstract int count();

    protected abstract void write(String fileName, String content);

    @Override public void close() {}
}
