package com.github.t1.wunderbar.junit.provider;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static com.github.t1.wunderbar.junit.http.HttpUtils.properties;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
class JarBarReader extends BarReader {
    private final URI uri;
    private final JarFile jarFile;

    JarBarReader(Path path) throws IOException {
        this.uri = path.toUri().normalize();
        this.jarFile = new JarFile(path.toFile());
    }

    @Override protected Stream<TreeEntry> treeEntries() {
        return jarFile.stream().flatMap(this::treeEntry);
    }

    private Stream<TreeEntry> treeEntry(ZipEntry zipEntry) {
        var name = zipEntry.getName();
        var matcher = ENTRY_PATTERN.matcher(name);
        if (!matcher.matches()) {
            log.info("skipping unexpected file {}", name);
            return Stream.empty();
        }

        var path = Path.of(matcher.group("path"));
        var number = Integer.parseInt(matcher.group("number"));

        // referencing the directories within archives would be nice, but at least IntelliJ doesn't support `!` nor `#`,
        // i.e. you can't re-run selected tests any more
        // var uri = URI.create(this.uri + "!" + path); // using URI#resolve would remove parts of the original path
        return Stream.of(new TreeEntry(path, number, uri));
    }

    @Override String getDisplayName() {
        var comment = properties(jarFile.getComment());
        var fileName = Path.of(jarFile.getName()).getFileName().toString();
        return fileName + Optional.ofNullable(comment.getProperty("version")).map(version -> " [v" + version + "]").orElse("");
    }


    @Override @SneakyThrows(IOException.class)
    protected Optional<String> optionalRead(String name) {
        var entry = jarFile.getEntry(name);
        if (entry == null) return Optional.empty();
        try (var scanner = new Scanner(jarFile.getInputStream(entry), UTF_8)) {
            return Optional.of(scanner.useDelimiter("\\Z").next());
        }
    }


    private static final Pattern ENTRY_PATTERN = Pattern.compile("(?<path>.*)/(?<number>\\d+) .*");
}
