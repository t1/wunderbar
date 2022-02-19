package com.github.t1.wunderbar.junit.consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.common.Utils.deleteRecursive;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.list;
import static java.nio.file.Files.writeString;

class DirectoryBarWriter extends BarWriter {
    @Getter private final Path path;
    @Setter private String comment;
    @Getter @Setter private String directory;

    DirectoryBarWriter(Path path) {
        this.path = path;
        deleteRecursive(path);
    }

    private Path currentDir() {return path.resolve(directory);}

    @Override public AtomicInteger counter() {return new AtomicInteger(currentCount());}

    @SneakyThrows(IOException.class)
    private int currentCount() {
        if (directory == null)
            throw new IllegalStateException("must set directory before calling save: " + this);
        if (!exists(currentDir())) return 0;
        try (var files = list(currentDir())) {
            return files.mapToInt(this::fileNumber).max().orElse(0);
        }
    }

    private int fileNumber(Path path) {
        var fileName = path.getFileName().toString();
        var matcher = Pattern.compile("(?<number>\\d+) .*").matcher(fileName);
        if (!matcher.matches()) return 0;
        return Integer.parseInt(matcher.group("number"));
    }

    @SneakyThrows(IOException.class)
    @Override protected void write(String fileName, String content) {
        var filePath = path.resolve(fileName);
        assert filePath.getParent().equals(currentDir());
        createDirectories(currentDir());
        writeString(filePath, content);
    }

    @Override public void close() {
        directory = "";
        write("comment.yaml", comment);
    }
}
