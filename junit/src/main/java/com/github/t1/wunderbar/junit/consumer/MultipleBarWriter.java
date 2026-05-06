package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class MultipleBarWriter extends BarWriter {
    private final List<BarWriter> delegates;

    MultipleBarWriter(List<BarWriter> delegates) {this.delegates = delegates;}

    @Override public void setGeneratedDataPoints(List<GeneratedDataPoint> generatedDataPoints) {
        super.setGeneratedDataPoints(generatedDataPoints);
        delegates.forEach(delegate -> delegate.setGeneratedDataPoints(generatedDataPoints));
    }

    @Override public Path getPath() {return delegates.getFirst().getPath();}

    @Override protected void setComment(String directory) {delegates.forEach(delegate -> delegate.setComment(directory));}

    @Override public void setDirectory(String directory) {delegates.forEach(delegate -> delegate.setDirectory(directory));}

    @Override public String getDirectory() {return delegates.getFirst().getDirectory();}

    @Override public void save(HttpRequest request, HttpResponse response) {delegates.forEach(delegate -> delegate.save(request, response));}

    @Override public AtomicInteger counter() {return delegates.getFirst().counter();}

    @Override protected void write(String fileName, String content) {throw new UnsupportedOperationException();}

    @Override public void close() {/* underlying writers are closed by the extension shutdown hook */}
}
