package test.provider;

import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiProvider(baseUri = "{endpoint()}")
class SetupTeardownAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();

    @SuppressWarnings("unused")
    URI endpoint() { return dummyServer.baseUri(); }

    List<String> called = new ArrayList<>();

    @BeforeEach void beforeEach() { called.add("beforeEach"); }

    @BeforeDynamicTest void beforeWithoutArgs() { called.add("beforeWithoutArgsCalled"); }

    @BeforeDynamicTest void beforeWithListArg(List<HttpServerInteraction> list) { called.add("beforeWithListArgCalled"); }


    @BeforeInteraction void beforeInteractionWithoutArg() { called.add("beforeInteractionWithoutArgCalled"); }

    @BeforeInteraction void beforeInteractionWithArg(HttpServerInteraction interaction) { called.add("beforeInteractionWithArgCalled"); }


    @AfterInteraction void afterInteractionWithoutArg() { called.add("afterInteractionWithoutArgCalled"); }

    @AfterInteraction void afterInteractionWithArg(HttpServerInteraction interaction) { called.add("afterInteractionWithArgCalled"); }


    @AfterDynamicTest void afterWithListArg(List<HttpServerInteraction> list) { called.add("afterWithListArgCalled"); }

    @AfterDynamicTest void afterWithoutArgs() { called.add("afterWithoutArgsCalled"); }


    @TestFactory DynamicNode consumerTests() {
        return findTestsIn("src/test/resources/health");
    }

    @AfterEach void tearDown() {
        then(called).containsExactly(
            "beforeEach",
            "beforeWithoutArgsCalled", "beforeWithListArgCalled",
            "beforeInteractionWithoutArgCalled", "beforeInteractionWithArgCalled",
            "afterInteractionWithoutArgCalled", "afterInteractionWithArgCalled",
            "afterWithListArgCalled", "afterWithoutArgsCalled");
    }
}
