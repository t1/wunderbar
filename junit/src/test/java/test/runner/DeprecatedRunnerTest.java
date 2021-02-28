package test.runner;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;

@SuppressWarnings("removal")
@com.github.t1.wunderbar.junit.runner.WunderBarRunnerExtension(baseUri = "dummy")
class DeprecatedRunnerTest {
    @RegisterExtension static RunnerFixture fixture = new RunnerFixture();

    @TestFactory DynamicNode flatTest() {
        fixture.withTest("flat")

            .expect("flat", 1);

        return fixture.findTests();
    }
}
