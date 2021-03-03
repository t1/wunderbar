package test.provider;

import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeDynamicTest;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

// @WunderBarApiProvider(baseUri = "dummy")
public class InvalidApiProviderTest {
    // we can't check the exception thrown for the missing WunderBarApiProvider annotation
    // we can't check for the exception thrown for invalid argument type
    private static final boolean DISABLED = true;

    @BeforeDynamicTest void beforeWithInvalidArg(int x) {}

    @AfterDynamicTest void afterWithInvalidArg(int x) {}

    @TestFactory DynamicNode dummy() {
        if (DISABLED) return dynamicTest("dummy", () -> {});
        return findTestsIn("src/test/resources/health");
    }
}
