package test.service;

import com.github.t1.wunderbar.junit.Bar;
import com.github.t1.wunderbar.junit.WunderBarExecutorJUnit;
import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.junit.WunderBarExecutorJUnit.DEFAULT_TEST_FACTORY;
import static com.github.t1.wunderbar.junit.WunderBarExecutorJUnit.TEST_FACTORY;
import static com.github.t1.wunderbar.junit.WunderBarExecutorJUnit.Test;
import static org.assertj.core.api.BDDAssertions.then;

class ProductServiceAcceptanceTest {
    static HttpServerResponse response;
    static HttpServerRequest request;
    static final HttpServer httpServer = new HttpServer(request1 -> {
        assert request1 == request;
        return response;
    });

    static @TempDir Path tmp;

    List<Test> expected = new ArrayList<>();
    List<Test> executed = new ArrayList<>();

    @BeforeEach void setUp() { TEST_FACTORY = test -> () -> executed.add(test); }

    @AfterEach void tearDown() {
        TEST_FACTORY = DEFAULT_TEST_FACTORY;
        then(executed).describedAs("executed tests").containsExactlyElementsOf(expected);
    }

    @AfterAll static void afterAll() { httpServer.stop(); }

    @TestFactory DynamicNode standardTests() {
        var bar = new BarBuilder("standard-behavior")
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

        expect("some-test", 1);

        return new WunderBarExecutorJUnit(bar.getPath()).build();
    }

    @TestFactory DynamicNode nestedTests() {
        var bar = new BarBuilder("nesting-behavior")
            .with("root-1")
            .with("root-2")
            .with("root-3")
            .with("root-3")
            .with("root/flat-1")
            .with("root/flat-2")
            .with("root/flat-3")
            .with("root/nested/nest-1")
            .with("root/deeply/nested/deep-1")
            .with("root/deeply/nested/deep-2")
            .build();

        expect("root-1", 1);
        expect("root-2", 1);
        expect("root-3", 1);
        // expect("root-3", 2);
        expect("flat-1", 1);
        expect("flat-2", 1);
        expect("flat-3", 1);
        expect("nest-1", 1);
        expect("deep-1", 1);
        expect("deep-2", 1);

        return new WunderBarExecutorJUnit(bar.getPath()).build();
    }

    static class BarBuilder {
        private final Bar bar;
        private int nextTestValue = 0;

        BarBuilder(String name) {
            this.bar = new Bar(name);
            bar.setPath(tmp.resolve("wunder.bar"));
        }

        BarBuilder with(String directory) { return with(directory, Integer.toString(nextTestValue++)); }

        BarBuilder with(String directory, String value) {
            return with(directory, ProductServiceAcceptanceTest.request(value), ProductServiceAcceptanceTest.response(value));
        }

        BarBuilder with(String directory, HttpServerRequest request, HttpServerResponse response) {
            bar.setDirectory(directory);
            ProductServiceAcceptanceTest.request = request;
            ProductServiceAcceptanceTest.response = response;
            bar.save(r -> response).apply(request);
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

    private boolean expect(String path, int number) {
        return expected.add(new Test(Path.of(path), number));
    }
}
