package test.runner;

import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import com.github.t1.wunderbar.junit.runner.AfterDynamicTest;
import com.github.t1.wunderbar.junit.runner.BeforeDynamicTest;
import com.github.t1.wunderbar.junit.runner.WunderBarRunnerExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;
import java.util.List;

import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarRunnerExtension(baseUri = "{endpoint()}")
class SetupTeardownAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();

    @SuppressWarnings("unused")
    URI endpoint() { return dummyServer.baseUri(); }

    boolean beforeWithoutArgsCalled;
    boolean beforeWithListArgCalled;
    boolean afterWithListArgCalled;
    boolean afterWithoutArgsCalled;

    @BeforeDynamicTest void beforeWithoutArgs() { beforeWithoutArgsCalled = true; }

    @BeforeDynamicTest void beforeWithListArg(List<HttpServerInteraction> list) { beforeWithListArgCalled = true; }

    @AfterDynamicTest void afterWithListArg(List<HttpServerInteraction> list) { afterWithListArgCalled = true; }

    @AfterDynamicTest void afterWithoutArgs() { afterWithoutArgsCalled = true; }


    @TestFactory DynamicNode consumerTests() {
        return findTestsIn("src/test/resources/health");
    }

    @AfterEach void tearDown() {
        then(beforeWithoutArgsCalled).describedAs("beforeWithoutArgsCalled").isTrue();
        then(beforeWithListArgCalled).describedAs("beforeWithListArgCalled").isTrue();
        then(afterWithListArgCalled).describedAs("afterWithListArgCalled").isTrue();
        then(afterWithoutArgsCalled).describedAs("afterWithoutArgsCalled").isTrue();
    }
}
