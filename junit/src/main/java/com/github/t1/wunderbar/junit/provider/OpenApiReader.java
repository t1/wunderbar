package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.junit.OpenApiContract;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

class OpenApiReader extends InteractionReader {
    private final Path path;
    private final OpenApiContract contract;
    private final Map<Path, OpenApiContract.TestCase> tests = new LinkedHashMap<>();

    @SneakyThrows(IOException.class)
    OpenApiReader(Path path) {
        this.path = path;
        this.contract = OpenApiContract.from(Files.readString(path, UTF_8));
        contract.tests().forEach(test -> tests.put(Path.of(test.path()), test));
    }

    @Override Stream<Test> tests() {
        var uri = path.toUri().normalize();
        return tests.values().stream()
                .map(test -> new Test(Path.of(test.path()), test.interactions().size(), uri));
    }

    @Override String getDisplayName() {return contract.displayName(path);}

    @Override List<HttpInteraction> interactionsFor(Test test) {
        var testCase = tests.get(test.path());
        if (testCase == null) throw new WunderBarException("OpenAPI test not found: " + test.path());
        return testCase.interactions();
    }
}
