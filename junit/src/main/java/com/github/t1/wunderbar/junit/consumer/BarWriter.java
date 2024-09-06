package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.t1.wunderbar.junit.http.HttpUtils.properties;
import static com.github.t1.wunderbar.junit.http.HttpUtils.toJson;
import static java.util.Collections.emptyList;

@Setter @Slf4j
public @Internal abstract class BarWriter implements Closeable {
    public static BarWriter to(String fileName) {
        Path path = Path.of(fileName);
        var archiveComment = "version: 1.1\n";
        log.info("create bar [{}] in {}", archiveComment, fileName);
        var barWriter = fileName.endsWith("/") ? new DirectoryBarWriter(path) : new JarBarWriter(path);
        barWriter.setComment(archiveComment);
        return barWriter;
    }

    private List<GeneratedDataPoint> generatedDataPoints = emptyList(); // this class won't change it

    @Override public String toString() {return getClass().getSimpleName() + ":" + getPath();}

    public abstract Path getPath();

    protected abstract void setComment(String directory);

    public abstract void setDirectory(String directory);

    public abstract String getDirectory();

    public final void save(HttpRequest request, HttpResponse response) {
        String id = getDirectory() + "/" + counter().incrementAndGet() + " ";
        writeRequestFiles(request, id);
        writeResponseFiles(response, id);
    }

    private void writeRequestFiles(HttpRequest request, String id) {
        if (request.getAuthorization() != null)
            request = request.withAuthorization(request.getAuthorization().toDummy());
        var headers =
                "Method: " + request.getMethod() + "\n" +
                "URI: " + request.getUri() + "\n" +
                request.headerProperties();
        write(id + "request-headers.properties", headers);
        request.body().ifPresent(body -> writeBody(id + "request", body));
        writeVariables(id + "request", request.body(), properties(headers));
    }

    private void writeResponseFiles(HttpResponse response, String id) {
        write(id + "response-headers.properties", response.headerProperties());
        response.body().ifPresent(body -> writeBody(id + "response", body));
        writeVariables(id + "response", response.body(), properties(response.headerProperties()));
    }

    private void writeBody(String fileNamePrefix, String body) {
        write(fileNamePrefix + "-body.json", body);
    }

    private void writeVariables(String fileNamePrefix, Optional<String> optionalBody, Properties headers) {
        var variables = new HashMap<String, GeneratedDataPoint>();
        var variablesCollector = new VariablesCollector(generatedDataPoints, variables, "");
        optionalBody.ifPresent(variablesCollector::collectBody);
        variablesCollector.collect(headers);
        if (!variables.isEmpty())
            write(fileNamePrefix + "-variables.json", toJson(variables));
    }

    public abstract AtomicInteger counter();

    protected abstract void write(String fileName, String content);

    @Override public abstract void close();
}
