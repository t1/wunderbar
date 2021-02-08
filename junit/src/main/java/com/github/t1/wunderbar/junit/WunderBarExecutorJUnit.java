package com.github.t1.wunderbar.junit;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class WunderBarExecutorJUnit {
    public static final Function<Test, Executable> DEFAULT_TEST_FACTORY = BarExecutor::new;
    /** visible for testing */
    public static Function<Test, Executable> TEST_FACTORY = DEFAULT_TEST_FACTORY;
    private final JarFile jarFile;
    private final TestNode tests;

    private interface TestNode {
        Path getPath();

        DynamicNode map();

        void merge(TestNode node);
    }

    private static @Value class TestCollection implements TestNode {
        @NonNull Path path;
        List<TestNode> children = new ArrayList<>();

        @Override public String toString() { return path + ": " + children; }

        public TestNode with(TestNode child) {
            children.add(child);
            return this;
        }

        @Override public void merge(TestNode that) {
            if (that instanceof TestCollection && this.getPath().equals(that.getPath())) {
                ((TestCollection) that).children.forEach(this::merge);
            } else {
                var child = children.stream()
                    .filter(c -> c.getPath().equals(that.getPath()))
                    .findAny();
                if (child.isPresent())
                    child.get().merge(that);
                else children.add(that);
            }
        }

        @Override public DynamicNode map() {
            return dynamicContainer(path.toString(), children.stream().map(TestNode::map));
        }
    }

    public static @Value class Test implements TestNode {
        private static final Pattern PATTERN = Pattern.compile("(?<path>.*)/(?<number>\\d+) .*");

        private static TestNode of(Matcher matcher) {
            var path = Path.of(matcher.group("path"));
            TestNode node = new Test(path.getFileName(), Integer.parseInt(matcher.group("number")));
            for (int i = path.getNameCount() - 2; i >= 0; i--)
                node = new TestCollection(path.getName(i)).with(node);
            return node;
        }

        @NonNull Path path;
        int number;

        @Override public String toString() { return path + "#" + number; }

        @Override public void merge(TestNode node) {
            var collection = new TestCollection(node.getPath());
            collection.children.add(this);
            collection.children.add(node);
        }

        @Override public DynamicNode map() { return dynamicTest(toString(), TEST_FACTORY.apply(this)); }
    }

    @SneakyThrows(IOException.class)
    public WunderBarExecutorJUnit(Path barFilePath) {
        this.jarFile = new JarFile(barFilePath.toFile());
        this.tests = scanTests();
    }

    private TestNode scanTests() {
        var root = new TestCollection(Path.of(getName())); // TODO use a Collector instead
        jarFile.stream()
            .map(ZipEntry::getName)
            .map(Test.PATTERN::matcher)
            .filter(Matcher::matches)
            .map(Test::of)
            .distinct()
            .forEach(root::merge);
        return root;
    }

    public DynamicNode build() { return tests.map(); }

    public String getName() {
        var comment = jarFile.getComment();
        return (comment != null) ? comment : jarFile.getName();
    }

    @SneakyThrows(IOException.class)
    private static String read(JarFile file, JarEntry entry) {
        try (var inputStream = file.getInputStream(entry)) {
            return new Scanner(inputStream, UTF_8).useDelimiter("\\Z").next();
        }
    }
}
