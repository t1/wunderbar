package test.runner;

import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import com.github.t1.wunderbar.junit.runner.WunderBarRunnerExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.Utils.deleteRecursive;
import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.Test;
import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsIn;
import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsInArtifact;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarRunnerExtension(baseUri = "health")
class RunnerTest {
    static HttpServerResponse response;
    static HttpServerRequest request;
    static final HttpServer httpServer = new HttpServer(request1 -> {
        assert request1 == request;
        return response;
    });

    static @TempDir Path tmp;

    List<Test> expected = new ArrayList<>();
    List<Test> executed = new ArrayList<>();

    Function<Test, Executable> executionCollector = test -> () -> executed.add(test);

    @AfterEach void finish() {
        then(executed).describedAs("executed tests").containsExactlyElementsOf(expected);
    }

    @AfterAll static void afterAll() { httpServer.stop(); }

    @TestFactory DynamicNode standardTest() {
        var fixture = new Fixture()
            .withTest("some-container/some-test",
                HttpServerRequest.builder()
                    .method("POST")
                    .body("{\n" +
                        "  \"query\":\"query product($id: String) { product(id: $id) {id name} }\",\n" +
                        "  \"variables\":{\"id\":\"x\"}\n" +
                        "}")
                    .build(),
                HttpServerResponse.builder()
                    .body("{\n" +
                        "  \"data\": {\n" +
                        "    \"product\": {\"id\": \"x\",\"name\": \"some-product-name\"}\n" +
                        "  },\n" +
                        "  \"errors\": []\n" +
                        "}\n")
                    .build())

            .expect("some-container/some-test", 1, "wunder.bar");

        return findTestsIn(fixture.bar.getPath(), executionCollector);
    }

    @TestFactory DynamicNode nestedTests() {
        var fixture = new Fixture()
            .withTest("root-1")
            .withTest("root-2")
            .withTest("root-2")
            .withTest("root-2")
            .withTest("root/flat-1")
            .withTest("root/flat-2")
            .withTest("root/flat-3")
            .withTest("root/nested/nest-1")
            .withTest("root/deeply/nested/deep-1")
            .withTest("root/deeply/nested/deep-2")
            .withTest("root-3")

            .expect("root-1", 1, "wunder.bar")
            .expect("root-2", 3, "wunder.bar")
            .expect("root/flat-1", 1, "wunder.bar")
            .expect("root/flat-2", 1, "wunder.bar")
            .expect("root/flat-3", 1, "wunder.bar")
            .expect("root/nested/nest-1", 1, "wunder.bar")
            .expect("root/deeply/nested/deep-1", 1, "wunder.bar")
            .expect("root/deeply/nested/deep-2", 1, "wunder.bar")
            .expect("root-3", 1, "wunder.bar");

        return findTestsIn(fixture.bar.getPath(), executionCollector);
    }

    @TestFactory DynamicNode flatTest() {
        var fixture = new Fixture().withTest("flat")

            .expect("flat", 1, "wunder.bar");

        return findTestsIn(fixture.bar.getPath(), executionCollector);
    }

    @Nested class FindInArtifact {
        Path tmpDir;
        Path versionDir;
        final String coordinates = "com.github.t1:wunderbar.test.artifact:1.2.3";

        @BeforeEach void setUp() throws IOException {
            tmpDir = Path.of(System.getProperty("user.home")).resolve(".m2/repository/com/github/t1/wunderbar.test.artifact");
            versionDir = tmpDir.resolve("1.2.3");
            Files.createDirectories(versionDir);
        }

        @AfterEach
        void tearDown() { deleteRecursive(tmpDir); }

        @TestFactory DynamicNode artifactTestWithSpecifiedClassifierAndType() {
            new Fixture(versionDir.resolve("wunderbar.test.artifact-1.2.3-bar.jar"))
                .withTest("artifact-test")

                .expect("artifact-test", 1, "wunderbar.test.artifact-1.2.3-bar.jar");

            return findTestsInArtifact(coordinates + ":bar:jar", executionCollector);
        }

        @TestFactory DynamicNode artifactTestWithDefaultClassifierAndSpecifiedType() {
            new Fixture(versionDir.resolve("wunderbar.test.artifact-1.2.3.jar"))
                .withTest("artifact-test")

                .expect("artifact-test", 1, "wunderbar.test.artifact-1.2.3.jar");

            return findTestsInArtifact(coordinates + ":jar", executionCollector);
        }

        @TestFactory DynamicNode artifactWithDefaultClassifierAndTypeTest() {
            new Fixture(versionDir.resolve("wunderbar.test.artifact-1.2.3-bar.bar"))
                .withTest("artifact-test")

                .expect("artifact-test", 1, "wunderbar.test.artifact-1.2.3-bar.bar");

            return findTestsInArtifact(coordinates, executionCollector);
        }
    }

    class Fixture {
        private final BarWriter bar;
        private int nextTestValue = 0;
        private boolean closed = false;

        Fixture() { this(tmp.resolve("wunder.bar")); }

        Fixture(Path path) {
            this.bar = BarWriter.to(path.toString());
        }

        Fixture withTest(String directory) { return withTest(directory, Integer.toString(nextTestValue++)); }

        Fixture withTest(String directory, String value) {
            return withTest(directory, request(value), response(value));
        }

        Fixture withTest(String directory, HttpServerRequest request, HttpServerResponse response) {
            assert !closed;
            bar.setDirectory(directory);
            RunnerTest.request = request;
            RunnerTest.response = response;
            bar.save(request, response);
            return this;
        }

        Fixture expect(String path, int number, String displayName) {
            if (!closed) {
                closed = true;
                bar.close();
            }

            expected.add(new Test(Path.of(path), number, displayName, bar.getPath().toUri()));

            return this;
        }
    }


    private static HttpServerRequest request(String value) {
        return HttpServerRequest.builder().body("{\"value\":\"" + value + "\"}").build();
    }

    private static HttpServerResponse response(String value) {
        return HttpServerResponse.builder().body("{\"value\":\"" + value + "\"}").build();
    }
}
