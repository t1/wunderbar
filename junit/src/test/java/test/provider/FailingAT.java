package test.provider;

import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;

import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;

@SuppressWarnings("unused")
@WunderBarApiProvider(baseUri = "{endpoint()}")
class FailingAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();

    @SuppressWarnings("unused")
    URI endpoint() { return dummyServer.baseUri(); }

    // @TestFactory // we can't check the assertion error thrown for the unexpected error
    // this would also cover JsonValueAssert
    DynamicNode failingConsumerTests() {
        return findTestsIn("src/test/resources/failing-wunder-bar");
    }
}
