package test.runner;

import com.github.t1.wunderbar.junit.runner.WunderBarRunner;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;

import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsIn;

@WunderBarRunner(baseUri = "{endpoint()}")
class ConsumerDrivenAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();

    @SuppressWarnings("unused")
    URI endpoint() { return dummyServer.baseUri(); }


    @TestFactory DynamicNode consumerTests() {
        return findTestsIn("src/test/resources/test-wunder-bar");
    }
}
