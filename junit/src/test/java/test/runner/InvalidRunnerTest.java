package test.runner;

import com.github.t1.wunderbar.junit.runner.AfterDynamicTest;
import com.github.t1.wunderbar.junit.runner.BeforeDynamicTest;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsIn;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

// @WunderBarRunnerExtension(baseUri = "dummy")
public class InvalidRunnerTest {
    // we can't check the exception thrown for the missing WunderBarRunnerExtension annotation
    // we can't check for the exception thrown for invalid argument type
    private static final boolean DISABLED = true;

    @BeforeDynamicTest void beforeWithInvalidArg(int x) {}

    @AfterDynamicTest void afterWithInvalidArg(int x) {}

    @TestFactory DynamicNode dummy() {
        if (DISABLED) return dynamicTest("dummy", () -> {});
        return findTestsIn("src/test/resources/health");
    }
}
