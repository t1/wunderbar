package com.github.t1.wunderbar.junit;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

public class WunderBarJUnitExecutor {
    public static DynamicNode findTestsIn(String barPath) {return findTestsIn(Path.of(barPath));}

    public static DynamicNode findTestsIn(Path barPath) { return findTestsIn(barPath, null); }

    public static DynamicNode findTestsIn(Path barPath, Function<Test, Executable> executableFactory) {
        return new WunderBarJUnitExecutor(barPath, executableFactory).root();
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
            return dynamicTest(getName(), executableFactory.apply(this));
        }

        public String requestHeaders() { return filePrefix() + "request-headers.properties"; }

        public String requestBody() { return filePrefix() + "request-body.json"; }

        public String responseHeaders() { return filePrefix() + "response-headers.properties"; }

        public String responseBody() { return filePrefix() + "response-body.json"; }

        private String filePrefix() { return getPath().getParent().toString() + "/" + getName() + " "; }
    }

    @SneakyThrows(IOException.class)
    private WunderBarJUnitExecutor(Path barFilePath, Function<Test, Executable> executableFactory) {
        // the indirection with null is necessary, as we can't access `this` in the `this()` constructor chain
        this.executableFactory = (executableFactory == null) ? test -> new BarExecutor(this, test) : executableFactory;
        this.jarFile = new JarFile(barFilePath.toFile());
        this.root = new TestCollection(barFilePath.toUri(), Path.of(getName()));

        scanTests();
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

    @SneakyThrows(IOException.class)
    String read(String name) {
        var entry = jarFile.getEntry(name);
        if (entry == null) throw new JUnitWunderBarException("bar entry " + name + " not found");
        try (var inputStream = jarFile.getInputStream(entry)) {
            return new Scanner(inputStream, UTF_8).useDelimiter("\\Z").next();
        }
    }
}
