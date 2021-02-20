package test.runner;

import com.github.t1.wunderbar.junit.Bar;
import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import com.github.t1.wunderbar.junit.runner.WunderBarRunnerExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.Test;
import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarRunnerExtension(baseUri = "dummy")
class RunnerTest {
    static HttpServerResponse response;
    static HttpServerRequest request;
    static final HttpServer httpServer = new HttpServer(request1 -> {
        assert request1 == request;
        return response;
    });

    static @TempDir Path tmp;
    static Path wunderBarPath;

    List<Test> expected = new ArrayList<>();
    List<Test> executed = new ArrayList<>();

    Function<Test, Executable> executionCollector = test -> () -> executed.add(test);

    @AfterEach void tearDown() {
        then(executed).describedAs("executed tests").containsExactlyElementsOf(expected);
    }

    @AfterAll static void afterAll() { httpServer.stop(); }

    @BeforeEach
    void setUp() {
        wunderBarPath = tmp.resolve("wunder.bar");
    }

    @TestFactory DynamicNode standardTest() {
        var bar = new BarTestBuilder("standard-behavior")
            .with("some-container/some-test",
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
            .build();

        expect("some-container/some-test", 1, "some-test");

        return build(bar);
    }

    @TestFactory DynamicNode nestedTests() {
        var bar = new BarTestBuilder("nesting-behavior")
            .with("root-1")
            .with("root-2")
            .with("root-2")
            .with("root-2")
            .with("root/flat-1")
            .with("root/flat-2")
            .with("root/flat-3")
            .with("root/nested/nest-1")
            .with("root/deeply/nested/deep-1")
            .with("root/deeply/nested/deep-2")
            .with("root-3")
            .build();

        expect("root-1", 1, "root-1");
        expect("root-2", 3, "root-2");
        expect("root/flat-1", 1, "flat-1");
        expect("root/flat-2", 1, "flat-2");
        expect("root/flat-3", 1, "flat-3");
        expect("root/nested/nest-1", 1, "nest-1");
        expect("root/deeply/nested/deep-1", 1, "deep-1");
        expect("root/deeply/nested/deep-2", 1, "deep-2");
        expect("root-3", 1, "root-3");

        return build(bar);
    }

    @TestFactory DynamicNode flatTest() {
        var bar = new BarTestBuilder("nesting-behavior").with("flat").build();

        expect("flat", 1, "flat");

        return build(bar);
    }

    private DynamicNode build(Bar bar) { return findTestsIn(bar.getPath(), executionCollector); }

    static class BarTestBuilder {
        private final Bar bar;
        private int nextTestValue = 0;

        BarTestBuilder(String archiveComment) {
            this.bar = new Bar(archiveComment);
            bar.setPath(wunderBarPath);
        }

        BarTestBuilder with(String directory) { return with(directory, Integer.toString(nextTestValue++)); }

        BarTestBuilder with(String directory, String value) {
            return with(directory, request(value), response(value));
        }

        BarTestBuilder with(String directory, HttpServerRequest request, HttpServerResponse response) {
            bar.setDirectory(directory);
            RunnerTest.request = request;
            RunnerTest.response = response;
            bar.save(request, response);
            return this;
        }

        public Bar build() {
            bar.close();
            return bar;
        }
    }

    private static HttpServerRequest request(String value) {
        return HttpServerRequest.builder().body("{\"value\":\"" + value + "\"}").build();
    }

    private static HttpServerResponse response(String value) {
        return HttpServerResponse.builder().body("{\"value\":\"" + value + "\"}").build();
    }

    private void expect(String path, int number, String displayName) {
        expected.add(new Test(Path.of(path), number, displayName, wunderBarPath.toUri()));
    }
}
