package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public @Internal abstract class BarWriter implements Closeable {
    public static BarWriter to(String fileName) {
        Path path = Path.of(fileName);
        return fileName.endsWith("/") ? new DirectoryBarWriter(path) : new JarBarWriter(path);
    }

    @Override public String toString() {return getClass().getSimpleName() + ":" + getPath();}

    public abstract Path getPath();

    public abstract void setComment(String directory);

    public abstract void setDirectory(String directory);

    public abstract String getDirectory();

    public final void save(HttpRequest request, HttpResponse response) {
        String id = getDirectory() + "/" + counter().incrementAndGet() + " ";
        if (request.getAuthorization() != null) request = request.withAuthorization(request.getAuthorization().toDummy());
        write(id + "request-headers.properties", request.headerProperties());
        request.body().ifPresent(body -> write(id + "request-body.json", body));

        write(id + "response-headers.properties", response.headerProperties());
        response.body().ifPresent(body -> write(id + "response-body.json", body));
    }

    public abstract AtomicInteger counter();

    protected abstract void write(String fileName, String content);

    @Override public abstract void close();
}
