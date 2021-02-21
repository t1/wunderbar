package com.github.t1.wunderbar.junit.runner;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.Internal;
import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.function.Executable;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 *
 */
@Slf4j
public class WunderBarTestFinder {
    private static final Pattern FUNCTION = Pattern.compile("(?<prefix>.*)\\{(?<method>.*)\\(\\)}(?<suffix>.*)");


    public static DynamicNode findTestsIn(String barPath) {return findTestsIn(Path.of(barPath));}

    public static DynamicNode findTestsIn(Path barPath) { return findTestsIn(barPath, null); }

    public static DynamicNode findTestsIn(Path barPath, Function<Test, Executable> executableFactory) {
        return new WunderBarTestFinder(barPath, executableFactory).toDynamicNode();
    }


    private final Function<Test, Executable> executableFactory;
    private final BarReader bar;
    private final TestCollection root;

    private interface TestNode {
        Path getPath();

        DynamicNode toDynamicNode(Function<Test, Executable> executableFactory);
    }

    private static @Value class TestCollection implements TestNode {
        URI uri;
        @NonNull Path path;
        List<TestNode> children = new ArrayList<>();

        @Override public String toString() {
            return path + ": " + children.stream().map(TestNode::toString).collect(joining(", ", "[", "]"));
        }

        private void merge(Test test) {
            var collection = this;
            for (int i = 0; i < test.getPath().getNameCount() - 1; i++) {
                collection = collection.getOrCreateSubCollection(test.getPath().subpath(0, i + 1));
            }
            collection.addOrReplace(test);
        }

        private TestCollection getOrCreateSubCollection(Path path) {
            return children.stream()
                .filter(node -> node.getPath().equals(path))
                .findFirst()
                .flatMap(node -> (node instanceof TestCollection) ? Optional.of((TestCollection) node) : Optional.empty())
                .orElseGet(() -> {
                    var sub = new TestCollection(uri.resolve(path.toString()), path);
                    this.children.add(sub);
                    return sub;
                });
        }

        private void addOrReplace(Test newTest) {
            var existingTest = testAt(newTest.getPath());
            if (existingTest == null)
                children.add(newTest);
            else if (existingTest.getInteractionCount() < newTest.getInteractionCount())
                children.set(children.indexOf(existingTest), newTest);
            // else already contains the higher interactionCount
        }

        private Test testAt(Path path) {
            return (Test) children.stream() // if this cast fails, the file is badly corrupt
                .filter(child -> child.getPath().equals(path))
                .findFirst().orElse(null);
        }

        @Override public DynamicNode toDynamicNode(Function<Test, Executable> executableFactory) {
            var displayName = path.getFileName().toString();
            return dynamicContainer(displayName, uri, children.stream().map(child -> child.toDynamicNode(executableFactory)));
        }
    }

    public static @Internal @Value class Test implements TestNode {
        @NonNull Path path;
        int interactionCount;
        @NonNull String displayName;
        @NonNull URI uri;

        @Override public String toString() {
            return ((path.getParent() == null) ? "" : path.getParent() + " : ")
                + displayName + " [" + interactionCount + "]: " + uri;
        }

        @Override public DynamicNode toDynamicNode(Function<Test, Executable> executableFactory) {
            return dynamicTest(displayName, uri, executableFactory.apply(this));
        }
    }

    private WunderBarTestFinder(Path barFilePath, Function<Test, Executable> executableFactory) {
        if (WunderBarRunnerJUnitExtension.INSTANCE == null)
            throw new WunderBarException("annotate your wunderbar test with @" + WunderBarRunnerExtension.class.getName());

        this.bar = BarReader.of(barFilePath);
        this.root = new TestCollection(barFilePath.toUri().normalize(), Path.of(bar.getDisplayName()));

        // indirection with null is necessary, as we can't access `this` in the constructor chain to build the default factory
        this.executableFactory = (executableFactory == null)
            ? test -> new BarExecutable("\"" + bar.getDisplayName() + "\" : " + test, baseUri(), interactions(test))
            : executableFactory;

        scanTests();
    }

    private URI baseUri() {
        var extension = WunderBarRunnerJUnitExtension.INSTANCE;
        var baseUri = extension.settings.baseUri();
        var matcher = FUNCTION.matcher(baseUri);
        if (matcher.matches())
            baseUri = matcher.group("prefix") + extension.call(matcher.group("method")) + matcher.group("suffix");
        return URI.create(baseUri);
    }

    private void scanTests() {
        bar.tests().forEach(root::merge);
    }

    private DynamicNode toDynamicNode() { return root.toDynamicNode(executableFactory); }

    private List<HttpServerInteraction> interactions(Test test) {
        return IntStream.rangeClosed(1, test.getInteractionCount())
            .mapToObj(n -> new HttpServerInteraction(bar.request(test, n), bar.response(test, n)))
            .collect(toList());
    }
}
