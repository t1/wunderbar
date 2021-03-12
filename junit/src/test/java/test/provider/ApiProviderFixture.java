package test.provider;

import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.MavenCoordinates;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
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
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.BDDAssertions.then;

class ApiProviderFixture implements Extension, BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    private final Path tmp;

    private Path path;
    private BarWriter bar;
    private int nextTestValue = 0;

    private final List<Test> expected = new ArrayList<>();
    private final List<Test> executed = new ArrayList<>();

    private final Function<Test, Executable> EXECUTION_COLLECTOR = test -> () -> executed.add(test);


    @SneakyThrows(IOException.class)
    public ApiProviderFixture() {
        this.tmp = Files.createTempDirectory("wunderbar-fixture");
    }

    @Override public void beforeEach(ExtensionContext context) {
        this.path = tmp.resolve("wunder.bar");
        bar = null;
    }


    ApiProviderFixture in(Path path) {
        this.path = path;
        return this;
    }

    ApiProviderFixture withTest(String directory) { return withTest(directory, Integer.toString(nextTestValue++)); }

    ApiProviderFixture withTest(String directory, String value) {
        return withTest(directory, request(value), response(value));
    }

    ApiProviderFixture withTest(String directory, HttpRequest request, HttpResponse response) {
        if (bar == null) bar = BarWriter.to(path.toString());

        bar.setDirectory(directory);
        bar.save(request, response);

        return this;
    }

    ApiProviderFixture expect(String path, int count) {
        if (bar != null) bar.close();

        expected.add(new Test(Path.of(path), count, this.path.toUri()));

        return this;
    }


    private static HttpRequest request(String value) {
        return HttpRequest.builder().body("{\"value\":\"" + value + "\"}").build();
    }

    private static HttpResponse response(String value) {
        return HttpResponse.builder().body("{\"value\":\"" + value + "\"}").build();
    }


    public DynamicNode findTests() {
        return findTestsIn(bar.getPath(), EXECUTION_COLLECTOR);
    }

    public DynamicNode findTestsInArtifact(MavenCoordinates coordinates) {
        return WunderBarTestFinder.findTestsInArtifact(coordinates, EXECUTION_COLLECTOR);
    }

    @Override public void afterEach(ExtensionContext context) {
        if (path != null) deleteRecursive(path);
        then(executed).describedAs("executed tests").containsExactlyElementsOf(expected);
    }

    @Override public void afterAll(ExtensionContext context) {
        deleteRecursive(tmp);
    }
}
