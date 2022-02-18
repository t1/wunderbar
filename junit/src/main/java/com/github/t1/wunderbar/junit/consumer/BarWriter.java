package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumerJUnitExtension.GeneratedDataPoint;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;
import static com.github.t1.wunderbar.junit.http.HttpUtils.toJson;
import static java.util.Collections.emptyList;

public @Internal abstract class BarWriter implements Closeable {
    public static BarWriter to(String fileName) {
        Path path = Path.of(fileName);
        return fileName.endsWith("/") ? new DirectoryBarWriter(path) : new JarBarWriter(path);
    }

    @Setter private List<GeneratedDataPoint> generatedDataPoints = emptyList(); // this class won't change it

    @Override public String toString() {return getClass().getSimpleName() + ":" + getPath();}

    public abstract Path getPath();

    public abstract void setComment(String directory);

    public abstract void setDirectory(String directory);

    public abstract String getDirectory();

    public final void save(HttpRequest request, HttpResponse response) {
        String id = getDirectory() + "/" + counter().incrementAndGet() + " ";
        if (request.getAuthorization() != null) request = request.withAuthorization(request.getAuthorization().toDummy());
        write(id + "request-headers.properties", request.headerProperties());
        request.body().ifPresent(body -> write(id, "request", body));

        write(id + "response-headers.properties", response.headerProperties());
        response.body().ifPresent(body -> write(id, "response", body));
    }

    private void write(String id, String direction, String body) {
        write(id + direction + "-body.json", body);

        var variables = new HashMap<String, GeneratedDataPoint>();
        new VariablesCollector("", readJson(body)).collect(variables);
        if (!variables.isEmpty())
            write(id + direction + "-variables.json", toJson(variables));
    }

    @RequiredArgsConstructor
    private class VariablesCollector {
        private final String path;
        private final JsonValue json;

        public void collect(HashMap<String, GeneratedDataPoint> variables) {
            switch (json.getValueType()) {
                case STRING:
                    put(variables, ((JsonString) json).getString());
                    break;
                case NUMBER:
                    put(variables, ((JsonNumber) json).bigDecimalValue());
                    break;
                case OBJECT:
                    json.asJsonObject().forEach((key, field) -> new VariablesCollector(path + "/" + key, field)
                        .collect(variables));
                    break;
                case ARRAY:
                    var array = json.asJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        new VariablesCollector(path + "/" + i, array.get(i))
                            .collect(variables);
                    }
                    break;
                case NULL:
                case TRUE:
                case FALSE:
                    break; // these values can't be unique
            }
        }

        private void put(HashMap<String, GeneratedDataPoint> variables, Object value) {
            GeneratedDataPoint.find(BarWriter.this.generatedDataPoints, value)
                .ifPresent(generated -> variables.put(path, generated));
        }
    }

    public abstract AtomicInteger counter();

    protected abstract void write(String fileName, String content);

    @Override public abstract void close();
}
