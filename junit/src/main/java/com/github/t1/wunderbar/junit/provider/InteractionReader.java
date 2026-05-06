package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.junit.ContractFormat;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.github.t1.wunderbar.junit.ContractFormat.AUTO;
import static com.github.t1.wunderbar.junit.ContractFormat.BAR;
import static com.github.t1.wunderbar.junit.ContractFormat.OPENAPI;

abstract class InteractionReader {
    static InteractionReader from(Path path) {return from(path, AUTO);}

    @SneakyThrows(IOException.class)
    static InteractionReader from(Path path, ContractFormat format) {
        if (!Files.exists(path))
            throw new WunderBarException("can't find any tests in " + path);
        if (Files.isDirectory(path))
            return new DirBarReader(path);

        var resolvedFormat = (format == AUTO) ? resolve(path) : format;
        return switch (resolvedFormat) {
            case AUTO -> throw new IllegalStateException("unreachable");
            case BAR -> new JarBarReader(path);
            case OPENAPI -> new OpenApiReader(path);
        };
    }

    private static ContractFormat resolve(Path path) throws IOException {
        var fileName = path.getFileName().toString();
        if (fileName.endsWith(".json")) return OPENAPI;
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml"))
            throw new WunderBarException("OpenAPI YAML files are not supported yet: " + path);

        try (var input = Files.newInputStream(path)) {
            var bytes = input.readNBytes(1);
            return (bytes.length == 1 && bytes[0] == '{') ? OPENAPI : BAR;
        }
    }

    abstract Stream<Test> tests();

    abstract String getDisplayName();

    abstract List<HttpInteraction> interactionsFor(Test test);
}
