package com.github.t1.wunderbar.junit.runner;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class WunderBarTestFinder {
    public static DynamicNode findTestsIn(String barPath) {return findTestsIn(Path.of(barPath));}

    public static DynamicNode findTestsIn(Path barPath) { return findTestsIn(barPath, null); }

    public static DynamicNode findTestsIn(Path barPath, Function<Test, Executable> executableFactory) {
        return new WunderBarTestFinder(barPath, executableFactory).root();
    }


    private final Function<Test, Executable> executableFactory;
    private final JarFile jarFile;
    private final TestCollection root;

    private interface TestNode {
        String getName();

        boolean matches(TestNode that);

        DynamicNode map(Function<Test, Executable> executableFactory);
    }

    private static @Value class TestCollection implements TestNode {
        URI uri;
        @NonNull Path path;
        List<TestNode> children = new ArrayList<>();

        @Override public String getName() { return path.getFileName().toString(); }

        @Override public String toString() {
            return path + ": " + children.stream().map(TestNode::getName).collect(joining(", ", "[", "]"));
        }

        private TestNode with(TestNode child) {
            children.add(child);
            return this;
        }

        @Override public boolean matches(TestNode that) { return this.getName().equals(that.getName()); }

        private void merge(TestNode that) {
            if (that instanceof TestCollection && this.getName().equals(that.getName())) {
                ((TestCollection) that).children.forEach(this::merge);
            } else {
                var child = children.stream().filter(that::matches).findAny();
                if (child.isPresent())
                    ((TestCollection) child.get()).merge(that);
                else with(that);
            }
        }

        @Override public DynamicNode map(Function<Test, Executable> executableFactory) {
            return dynamicContainer(getName(), uri, children.stream().map(child -> child.map(executableFactory)));
        }
    }

    public static @Value class Test implements TestNode {
        private static final Pattern PATTERN = Pattern.compile("(?<path>.*)/(?<number>\\d+) .*");

        private static TestNode of(Matcher matcher) {
            var path = Path.of(matcher.group("path")).resolve(matcher.group("number"));
            TestNode node = new Test(path);
            for (int i = path.getNameCount() - 2; i >= 0; i--)
                node = new TestCollection(null, path.subpath(0, i + 1)).with(node);
            return node;
        }

        @NonNull Path path;

        @Override public String toString() { return getPath().toString(); }

        @Override public String getName() { return path.getFileName().toString(); }

        @Override public boolean matches(TestNode that) {
            return this.equals(that);
        }

        @Override public DynamicNode map(Function<Test, Executable> executableFactory) {
            return dynamicTest(path.getParent().getFileName() + "#" + getName(), executableFactory.apply(this));
        }

        private String filePrefix() { return getPath().getParent().toString() + "/" + getName() + " "; }
    }

    @SneakyThrows(IOException.class)
    private WunderBarTestFinder(Path barFilePath, Function<Test, Executable> executableFactory) {
        if (WunderBarRunnerJUnitExtension.INSTANCE == null)
            throw new WunderBarException("annotate your wunderbar test with @" + WunderBarRunnerExtension.class.getName());

        // the indirection with null is necessary, as we can't access `this` in the `this()` constructor chain
        this.executableFactory = (executableFactory == null) ? test -> new BarExecutor(barFilePath + " : " + test, baseUri(), interaction(test)) : executableFactory;
        this.jarFile = new JarFile(barFilePath.toFile());
        this.root = new TestCollection(barFilePath.toUri(), Path.of(getName()));

        scanTests();
    }

    private URI baseUri() {
        return URI.create(WunderBarRunnerJUnitExtension.INSTANCE.settings.baseUri());
    }

    private void scanTests() {
        jarFile.stream()
            .map(ZipEntry::getName)
            .map(Test.PATTERN::matcher)
            .filter(Matcher::matches)
            .map(Test::of)
            .distinct()
            .forEach(root::merge);
    }

    private DynamicNode root() { return root.map(executableFactory); }

    private String getName() {
        var comment = jarFile.getComment();
        return (comment != null) ? comment : jarFile.getName();
    }

    private HttpServerInteraction interaction(Test test) {
        return new HttpServerInteraction(request(test), response(test));
    }

    public HttpServerRequest request(Test test) { return HttpServerRequest.from(requestHeaders(test), requestBody(test)); }

    private Properties requestHeaders(Test test) { return properties(read(test.filePrefix() + "request-headers.properties")); }

    private Optional<String> requestBody(Test test) { return optionalRead(test.filePrefix() + "request-body.json"); }


    public HttpServerResponse response(Test test) { return HttpServerResponse.from(responseHeaders(test), responseBody(test)); }

    private Properties responseHeaders(Test test) { return properties(read(test.filePrefix() + "response-headers.properties")); }

    private Optional<String> responseBody(Test test) { return optionalRead(test.filePrefix() + "response-body.json"); }


    private String read(String name) {
        return optionalRead(name).orElseThrow(() -> new WunderBarException("bar entry " + name + " not found"));
    }

    @SneakyThrows(IOException.class)
    private Optional<String> optionalRead(String name) {
        var entry = jarFile.getEntry(name);
        if (entry == null) return Optional.empty();
        try (var inputStream = jarFile.getInputStream(entry)) {
            return Optional.of(new Scanner(inputStream, UTF_8).useDelimiter("\\Z").next());
        }
    }

    @SneakyThrows(IOException.class)
    private static Properties properties(String string) {
        var properties = new Properties();
        properties.load(new StringReader(string));
        return properties;
    }
}
