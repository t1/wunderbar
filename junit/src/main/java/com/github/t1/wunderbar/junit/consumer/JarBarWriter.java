package com.github.t1.wunderbar.junit.consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j @RequiredArgsConstructor
public class JarBarWriter extends BarWriter {
    @Getter final Path path;
    @Setter private String comment;
    @Getter @Setter private String directory;
    private JarOutputStream archive;
    private final Map<String, AtomicInteger> counters = new LinkedHashMap<>();

    @Override public String toString() {
        return "Bar[" + comment + ":" + path + " : " + directory + " : " + counters + ']';
    }

    @Override protected int count() {
        if (directory == null)
            throw new IllegalStateException("must set directory before calling save: " + this);
        return counters.computeIfAbsent(directory, i -> new AtomicInteger()).getAndIncrement();
    }

    @SneakyThrows(IOException.class)
    @Override protected void write(String fileName, String content) {
        var archive = archive();
        archive.putNextEntry(new ZipEntry(fileName));
        var bytes = content.getBytes(UTF_8);
        log.debug("write [{}] ({} bytes)", fileName, bytes.length);
        archive.write(bytes);
        archive.closeEntry();
    }

    @SneakyThrows(IOException.class)
    private JarOutputStream archive() {
        if (archive == null) {
            if (path == null) throw new IllegalStateException("expected path to be set");
            Files.deleteIfExists(path);
            var outputStream = Files.newOutputStream(path);
            archive = new JarOutputStream(outputStream);
            archive.setComment(comment);
        }
        return archive;
    }

    @Override @SneakyThrows(IOException.class)
    public void close() { if (archive != null) archive.close(); }
}
