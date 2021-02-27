package test.runner;

import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import com.github.t1.wunderbar.junit.runner.MavenCoordinates;
import com.github.t1.wunderbar.junit.runner.WunderBarTestFinder;
import com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.Test;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.Utils.deleteRecursive;
import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.BDDAssertions.then;

class RunnerFixture implements Extension, BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    private final Path tmp;

    private Path path;
    private BarWriter bar;
    private int nextTestValue = 0;

    private final List<Test> expected = new ArrayList<>();
    private final List<Test> executed = new ArrayList<>();

    private final Function<Test, Executable> EXECUTION_COLLECTOR = test -> () -> executed.add(test);


    @SneakyThrows(IOException.class)
    public RunnerFixture() {
        this.tmp = Files.createTempDirectory("wunderbar-runner");
    }

    @Override public void beforeEach(ExtensionContext context) {
        this.path = tmp.resolve("wunder.bar");
        bar = null;
    }


    RunnerFixture in(Path path) {
        this.path = path;
        return this;
    }

    RunnerFixture withTest(String directory) { return withTest(directory, Integer.toString(nextTestValue++)); }

    RunnerFixture withTest(String directory, String value) {
        return withTest(directory, request(value), response(value));
    }

    RunnerFixture withTest(String directory, HttpServerRequest request, HttpServerResponse response) {
        if (bar == null) bar = BarWriter.to(path.toString());

        bar.setDirectory(directory);
        bar.save(request, response);

        return this;
    }

    RunnerFixture expect(String path, int count) {
        if (bar != null) bar.close();

        expected.add(new Test(Path.of(path), count, this.path.toUri()));

        return this;
    }


    private static HttpServerRequest request(String value) {
        return HttpServerRequest.builder().body("{\"value\":\"" + value + "\"}").build();
    }

    private static HttpServerResponse response(String value) {
        return HttpServerResponse.builder().body("{\"value\":\"" + value + "\"}").build();
    }


    public DynamicNode findTests() {
        return findTestsIn(bar.getPath(), EXECUTION_COLLECTOR);
    }

    public DynamicNode findTestsInArtifact(MavenCoordinates coordinates) {
        return WunderBarTestFinder.findTestsInArtifact(coordinates, EXECUTION_COLLECTOR);
    }

    @Override public void afterEach(ExtensionContext context) {
        deleteRecursive(path);
        then(executed).describedAs("executed tests").containsExactlyElementsOf(expected);
    }

    @Override public void afterAll(ExtensionContext context) {
        deleteRecursive(tmp);
    }
}
