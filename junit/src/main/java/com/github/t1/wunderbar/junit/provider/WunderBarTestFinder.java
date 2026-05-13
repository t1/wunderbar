package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.ContractFormat;
import com.github.t1.wunderbar.junit.WunderBarException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.function.Executable;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.ContractFormat.AUTO;
import static com.github.t1.wunderbar.junit.ContractFormat.BAR;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/// Static methods to find WunderBar contract files, e.g. BAR or OpenAPI files.
///
/// BAR files are read with full fidelity. OpenAPI files are imported only as a simple, lossy subset suitable for
/// interoperability and straightforward provider checks.
///
/// See also: [WunderBarApiProvider]
@Slf4j
public class WunderBarTestFinder {
    /// Find all tests in that file. Usage:
    /// ```
    /// @TestFactory DynamicNode consumerDrivenContractTests() {
    ///     return findTestsIn("wunder.bar");
    ///     // or: return findTestsIn("openapi.json");
    /// }
    /// ```
    public static DynamicNode findTestsIn(String barPath) {return findTestsIn(Path.of(barPath));}

    /// Find all tests in that file using the given contract format.
    public static DynamicNode findTestsIn(String path, ContractFormat format) {return findTestsIn(Path.of(path), format);}

    /// Find all tests in that file. Usage:
    /// ```
    /// @TestFactory DynamicNode consumerDrivenContractTests() {
    ///     return findTestsIn("wunder.bar");
    ///     // or: return findTestsIn("openapi.json");
    /// }
    /// ```
    public static DynamicNode findTestsIn(Path barPath) {return findTestsIn(barPath, AUTO);}

    /// Find all tests in that file using the given contract format.
    public static DynamicNode findTestsIn(Path path, ContractFormat format) {return findTestsIn(path, format, null);}

    /// used for tests
    public static @Internal DynamicNode findTestsIn(Path path, ContractFormat format, Function<Test, Executable> executableFactory) {
        return new WunderBarTestFinder(path, format, executableFactory).toDynamicNode();
    }


    /// Find all tests in that maven artifact, downloading it from a maven repository with the `mvn` command
    /// when it's not already in the local repository. In this case, Maven has to be installed; the Maven configuration
    /// (mainly the `settings.xml`) is considered.
    ///
    /// The coordinates are a String consisting of:
    ///
    /// `<groupId>:<artifactId>:<version>[:<packaging>[:<classifier>]]`
    ///
    /// Note that both the `classifier` and the `packaging` (the file extension) are optional and default to `bar`.
    /// Use [OPENAPI][ContractFormat.OPENAPI] to default to `json`/`openapi` instead.
    public static DynamicNode findTestsInArtifact(String coordinates) {return findTestsInArtifact(MavenCoordinates.of(coordinates));}

    /// Find all tests in that maven artifact using the given contract format.
    public static DynamicNode findTestsInArtifact(String coordinates, ContractFormat format) {
        return findTestsInArtifact(MavenCoordinates.of(coordinates), format);
    }

    /// Find all tests in that maven artifact, downloading it from a maven repository with the `mvn` command
    /// when it's not already in the local repository. In this case, Maven has to be installed; the Maven configuration
    /// (mainly the `settings.xml`) is considered.
    ///
    /// Note that both the `classifier` and the `packaging` (the file extension) are optional and default to `bar`.
    /// Use [OPENAPI][ContractFormat.OPENAPI] to default to `json`/`openapi` instead.
    public static DynamicNode findTestsInArtifact(MavenCoordinates coordinates) {return findTestsInArtifact(coordinates, AUTO);}

    /// Find all tests in that maven artifact using the given contract format.
    public static DynamicNode findTestsInArtifact(MavenCoordinates coordinates, ContractFormat format) {
        return findTestsInArtifact(coordinates, format, null);
    }

    /// used for tests
    public static @Internal DynamicNode findTestsInArtifact(MavenCoordinates coordinates, ContractFormat format, Function<Test, Executable> executableFactory) {
        coordinates = withDefaults(coordinates, format);
        coordinates.download();
        return new WunderBarTestFinder(coordinates.getLocalRepositoryPath(), format, executableFactory).toDynamicNode();
    }

    private static MavenCoordinates withDefaults(MavenCoordinates coordinates, ContractFormat format) {
        var resolvedFormat = resolveArtifactFormat(coordinates, format);
        if (coordinates.getClassifier() == null)
            coordinates = coordinates.withClassifier(resolvedFormat.defaultClassifier());
        if (coordinates.getPackaging() == null)
            coordinates = coordinates.withPackaging(resolvedFormat.defaultPackaging());
        return coordinates;
    }

    private static ContractFormat resolveArtifactFormat(MavenCoordinates coordinates, ContractFormat format) {
        if (format != AUTO) return format;
        if (coordinates.getPackaging() == null) return BAR;
        return AUTO.resolve("x." + coordinates.getPackaging());
    }


    private final Function<Test, Executable> executableFactory;
    private final InteractionReader archive;
    private final TestCollection root;

    private interface TestNode {
        Path path();

        DynamicNode toDynamicNode(Function<Test, Executable> executableFactory);
    }

    private record TestCollection(
            URI uri,
            @NonNull Path path,
            List<TestNode> children) implements TestNode {
        @Override public @NonNull String toString() {
            return path + ": " + children.stream().map(TestNode::toString).collect(joining(", ", "[", "]"));
        }

        private void merge(Test test) {
            var collection = this;
            for (int i = 0; i < test.path().getNameCount() - 1; i++) {
                collection = collection.getOrCreateSubCollection(test.path().subpath(0, i + 1));
            }
            collection.addOrReplace(test);
        }

        private TestCollection getOrCreateSubCollection(Path path) {
            return children.stream()
                    .filter(node -> node.path().equals(path))
                    .findFirst()
                    .flatMap(node -> (node instanceof TestCollection) ? Optional.of((TestCollection) node) : Optional.empty())
                    .orElseGet(() -> {
                        var sub = new TestCollection(uri.resolve(path.toString()), path, new ArrayList<>());
                        this.children.add(sub);
                        return sub;
                    });
        }

        private void addOrReplace(Test newTest) {
            var existingTest = testAt(newTest.path());
            if (existingTest == null)
                children.add(newTest);
            else if (existingTest.interactionCount() < newTest.interactionCount())
                children.set(children.indexOf(existingTest), newTest);
            // else already contains the higher interactionCount
        }

        private Test testAt(Path path) {
            return (Test) children.stream() // if this cast fails, the file is badly corrupt
                    .filter(child -> child.path().equals(path))
                    .findFirst().orElse(null);
        }

        @Override public DynamicNode toDynamicNode(Function<Test, Executable> executableFactory) {
            var displayName = path.getFileName().toString();
            return dynamicContainer(displayName, uri, children.stream().map(child -> child.toDynamicNode(executableFactory)));
        }
    }

    public @Internal record Test(@NonNull Path path, int interactionCount, @NonNull URI uri) implements TestNode {
        @Override public @NonNull String toString() {return path + " [" + interactionCount + "] in " + uri;}

        public String getDisplayName() {return path.getFileName().toString();}

        @Override public DynamicNode toDynamicNode(Function<Test, Executable> executableFactory) {
            return dynamicTest(getDisplayName(), uri, executableFactory.apply(this));
        }
    }

    private WunderBarTestFinder(Path barFilePath, ContractFormat format, Function<Test, Executable> executableFactory) {
        if (WunderBarApiProviderJUnitExtension.INSTANCE == null)
            throw new WunderBarException("annotate your wunderbar test with @" + WunderBarApiProvider.class.getName());

        this.archive = InteractionReader.from(barFilePath, format);
        this.root = new TestCollection(barFilePath.toUri().normalize(), Path.of(archive.getDisplayName()), new ArrayList<>());

        // indirection with null is necessary, as we can't access `this` in the constructor chain to build the default factory
        this.executableFactory = (executableFactory == null)
                ? test -> WunderBarApiProviderJUnitExtension.INSTANCE.createExecutable(archive.interactionsFor(test), test)
                : executableFactory;

        scanTests();
    }

    private void scanTests() {
        archive.tests().forEach(root::merge);
    }

    private DynamicNode toDynamicNode() {return root.toDynamicNode(executableFactory);}
}
