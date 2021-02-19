package com.github.t1.wunderbar.junit.runner;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
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
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Slf4j
public class WunderBarTestFinder {
    public static DynamicNode findTestsIn(String barPath) {return findTestsIn(Path.of(barPath));}

    public static DynamicNode findTestsIn(Path barPath) { return findTestsIn(barPath, null); }

    public static DynamicNode findTestsIn(Path barPath, Function<Test, Executable> executableFactory) {
        return new WunderBarTestFinder(barPath, executableFactory).toDynamicNode();
    }


    private final Function<Test, Executable> executableFactory;
    private final JarFile jarFile;
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
            var collection = this.getOrCreateSubCollection(test.getPath().getName(0));
            for (int i = 1; i < test.getPath().getNameCount() - 1; i++) {
                collection = collection.getOrCreateSubCollection(test.getPath().subpath(0, i));
            }
            collection.addOrReplace(test);
        }

        private TestCollection getOrCreateSubCollection(Path path) {
            return children.stream()
                .filter(node -> node.getPath().equals(path))
                .findFirst()
                .flatMap(node -> (node instanceof TestCollection) ? Optional.of((TestCollection) node) : Optional.empty())
                .orElseGet(() -> {
                    var sub = new TestCollection(URI.create(uri + "/" + path), path);
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

    public static @Value class Test implements TestNode {
        @NonNull Path path;
        int interactionCount;
        String displayName;

        @Override public String toString() { return path.getParent() + " : " + displayName + " [" + interactionCount + "]"; }

        @Override public DynamicNode toDynamicNode(Function<Test, Executable> executableFactory) {
            return dynamicTest(displayName, executableFactory.apply(this));
        }
    }

    @SneakyThrows(IOException.class)
    private WunderBarTestFinder(Path barFilePath, Function<Test, Executable> executableFactory) {
        if (WunderBarRunnerJUnitExtension.INSTANCE == null)
            throw new WunderBarException("annotate your wunderbar test with @" + WunderBarRunnerExtension.class.getName());

        this.jarFile = new JarFile(barFilePath.toFile());
        this.root = new TestCollection(barFilePath.toUri().normalize(), Path.of(getDisplayName()));

        // indirection with null is necessary, as we can't access `this` in the constructor chain to build the default factory
        this.executableFactory = (executableFactory == null)
            ? test -> new BarExecutable("\"" + getDisplayName() + "\" : " + test, baseUri(), interactions(test))
            : executableFactory;

        scanTests();
    }

    private URI baseUri() {
        return URI.create(WunderBarRunnerJUnitExtension.INSTANCE.settings.baseUri());
    }

    private void scanTests() {
        jarFile.stream()
            .flatMap(this::treeEntry)
            .distinct() // remove duplicates for all the files for one test
            .map(TreeEntry::toTest)
            .forEach(root::merge);
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

        var comment = zipEntry.getComment();
        var fileName = path.getFileName().toString();
        String displayName = (comment == null) ? fileName : (comment + " [" + fileName + "]");

        return Stream.of(new TreeEntry(path, number, displayName));
    }

    private String getDisplayName() {
        var comment = jarFile.getComment();
        var fileName = Path.of(jarFile.getName()).getFileName().toString();
        return (comment == null) ? fileName : (comment + " [" + fileName + "]");
    }

    private DynamicNode toDynamicNode() { return root.toDynamicNode(executableFactory); }

    private List<HttpServerInteraction> interactions(Test test) {
        return IntStream.rangeClosed(1, test.getInteractionCount())
            .mapToObj(n -> new HttpServerInteraction(request(test, n), response(test, n)))
            .collect(toList());
    }

    public HttpServerRequest request(Test test, int n) { return HttpServerRequest.from(requestHeaders(test, n), requestBody(test, n)); }

    private Properties requestHeaders(Test test, int n) { return properties(read(test.getPath() + "/" + n + " request-headers.properties")); }

    private Optional<String> requestBody(Test test, int n) { return optionalRead(test.getPath() + "/" + n + " request-body.json"); }


    public HttpServerResponse response(Test test, int n) { return HttpServerResponse.from(responseHeaders(test, n), responseBody(test, n)); }

    private Properties responseHeaders(Test test, int n) { return properties(read(test.getPath() + "/" + n + " response-headers.properties")); }

    private Optional<String> responseBody(Test test, int n) { return optionalRead(test.getPath() + "/" + n + " response-body.json"); }


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

    private static final Pattern ENTRY_PATTERN = Pattern.compile("(?<path>.*)/(?<number>\\d+) .*");

    private static @Value class TreeEntry {
        Path path;
        int number;
        String displayName;

        private Test toTest() { return new Test(path, number, displayName); }
    }
}
