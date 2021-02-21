package com.github.t1.wunderbar.junit.runner;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@RequiredArgsConstructor
class DirectoryBarReader extends BarReader {
    private final Path rootPath;

    @SneakyThrows(IOException.class)
    @Override protected Stream<TreeEntry> treeEntries() {
        return Files.walk(rootPath).flatMap(this::treeEntry);
    }

    private Stream<TreeEntry> treeEntry(Path path) {
        if (Files.isDirectory(path)) return Stream.empty();

        var matcher = TEST_FILE.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            log.info("skipping unexpected file {}", path);
            return Stream.empty();
        }

        var relativePath = rootPath.relativize(path).getParent();
        var number = Integer.parseInt(matcher.group("number"));
        var displayName = relativePath.getFileName().toString();
        var uri = path.getParent().toUri();

        return Stream.of(new TreeEntry(relativePath, number, displayName, uri));
    }

    @Override String getDisplayName() {
        return rootPath.getFileName().toString();
    }

    @SneakyThrows(IOException.class)
    @Override protected Optional<String> optionalRead(String name) {
        var path = rootPath.resolve(name);
        if (Files.exists(path))
            return Optional.of(Files.readString(path, UTF_8));
        return Optional.empty();
    }

    private static final Pattern TEST_FILE = Pattern.compile("(?<number>[0-9]+) " +
        "(?<direction>(request|response))-(?<part>(body|headers))\\.(?<extension>(json|properties))");
}
