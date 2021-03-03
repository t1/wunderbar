package test.provider;

import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;

import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;

@WunderBarApiProvider(baseUri = "{endpoint()}")
class ConsumerDrivenAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();

    @SuppressWarnings("unused")
    URI endpoint() { return dummyServer.baseUri(); }


    @TestFactory DynamicNode consumerTestsInDir() {
        return findTestsIn("src/test/resources/test-wunder-bar");
    }

    @TestFactory DynamicNode consumerTestsInJar() {
        return findTestsIn("src/test/resources/test-wunder-bar.jar");
    }

    @TestFactory DynamicNode consumerTestsInBar() {
        return findTestsIn("src/test/resources/test-wunder-bar.bar");
    }
}
