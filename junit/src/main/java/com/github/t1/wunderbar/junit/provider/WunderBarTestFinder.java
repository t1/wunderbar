package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.WunderBarException;
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

import static com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.createExecutable;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Static methods to find <code>*.bar</code> files.
 *
 * @see WunderBarApiProvider
 */
@Slf4j
public class WunderBarTestFinder {
    /**
     * Find all tests in that file. Usage:
     * <pre><code>
     * &#64;TestFactory DynamicNode consumerDrivenContractTests() {
     *     return findTestsIn("wunder.bar");
     * }
     * </code></pre>
     */
    public static DynamicNode findTestsIn(String barPath) {return findTestsIn(Path.of(barPath));}

    /**
     * Find all tests in that file. Usage:
     * <pre><code>
     * &#64;TestFactory DynamicNode consumerDrivenContractTests() {
     *     return findTestsIn("wunder.bar");
     * }
     * </code></pre>
     */
    public static DynamicNode findTestsIn(Path barPath) {return findTestsIn(barPath, null);}

    /** used for tests */
    public static @Internal DynamicNode findTestsIn(Path barPath, Function<Test, Executable> executableFactory) {
        return new WunderBarTestFinder(barPath, executableFactory).toDynamicNode();
    }


    /**
     * Find all tests in that maven artifact, downloading it from a maven repository with the <code>mvn</code> command
     * when it's not already in the local repository. In this case, Maven has to be installed; the Maven configuration
     * (mainly the <code>settings.xml</code>) is considered.
     * <p>
     * The coordinates are a String consisting of:
     * <p>
     * <code>&lt;groupId&gt;:&lt;artifactId&gt;:&lt;version&gt;[:&lt;packaging&gt;[:&lt;classifier&gt;]]</code>
     * <p>
     * Note that both the <code>classifier</code> and the <code>packaging</code> (the file extension) are optional and default to <code>bar</code>.
     */
    public static DynamicNode findTestsInArtifact(String coordinates) {return findTestsInArtifact(MavenCoordinates.of(coordinates));}

    /**
     * Find all tests in that maven artifact, downloading it from a maven repository with the <code>mvn</code> command
     * when it's not already in the local repository. In this case, Maven has to be installed; the Maven configuration
     * (mainly the <code>settings.xml</code>) is considered.
     * <p>
     * Note that both the <code>classifier</code> and the <code>packaging</code> (the file extension) are optional and default to <code>bar</code>.
     */
    public static DynamicNode findTestsInArtifact(MavenCoordinates coordinates) {return findTestsInArtifact(coordinates, null);}

    /** used for tests */
    public static @Internal DynamicNode findTestsInArtifact(MavenCoordinates coordinates, Function<Test, Executable> executableFactory) {
        coordinates = withDefaults(coordinates);
        coordinates.download();
        return new WunderBarTestFinder(coordinates.getLocalRepositoryPath(), executableFactory).toDynamicNode();
    }

    private static MavenCoordinates withDefaults(MavenCoordinates coordinates) {
        if (coordinates.getClassifier() == null) coordinates = coordinates.withClassifier("bar");
        if (coordinates.getPackaging() == null) coordinates = coordinates.withPackaging("bar");
        return coordinates;
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
        @NonNull URI uri;

        @Override public String toString() {return path + " [" + interactionCount + "] in " + uri;}

        public String getDisplayName() {return path.getFileName().toString();}

        @Override public DynamicNode toDynamicNode(Function<Test, Executable> executableFactory) {
            return dynamicTest(getDisplayName(), uri, executableFactory.apply(this));
        }
    }

    private WunderBarTestFinder(Path barFilePath, Function<Test, Executable> executableFactory) {
        if (WunderBarApiProviderJUnitExtension.INSTANCE == null)
            throw new WunderBarException("annotate your wunderbar test with @" + WunderBarApiProvider.class.getName());

        this.bar = BarReader.from(barFilePath);
        this.root = new TestCollection(barFilePath.toUri().normalize(), Path.of(bar.getDisplayName()));

        // indirection with null is necessary, as we can't access `this` in the constructor chain to build the default factory
        this.executableFactory = (executableFactory == null)
            ? test -> createExecutable(bar.interactionsFor(test), test)
            : executableFactory;

        scanTests();
    }

    private void scanTests() {
        bar.tests().forEach(root::merge);
    }

    private DynamicNode toDynamicNode() {return root.toDynamicNode(executableFactory);}
}
