package com.github.t1.wunderbar.junit.consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.junit.Utils.deleteRecursive;

class DirectoryBarWriter extends BarWriter {
    @Getter private final Path path;
    @Setter private String comment;
    @Getter @Setter private String directory;

    DirectoryBarWriter(Path path) {
        this.path = path;
        deleteRecursive(path);
    }

    private Path currentDir() { return path.resolve(directory); }

    @SneakyThrows(IOException.class)
    @Override protected int count() {
        if (directory == null)
            throw new IllegalStateException("must set directory before calling save: " + this);
        if (Files.isDirectory(currentDir()))
            return Files.list(currentDir()).mapToInt(this::fileNumber).max().orElse(0);
        return 0;
    }

    private int fileNumber(Path path) {
        var fileName = path.getFileName().toString();
        var matcher = Pattern.compile("(?<number>[0-9]+) .*").matcher(fileName);
        if (!matcher.matches()) return 0;
        return Integer.parseInt(matcher.group("number"));
    }

    @SneakyThrows(IOException.class)
    @Override protected void write(String fileName, String content) {
        var filePath = path.resolve(fileName);
        assert filePath.getParent().equals(currentDir());
        Files.createDirectories(currentDir());
        Files.writeString(filePath, content);
    }
}
