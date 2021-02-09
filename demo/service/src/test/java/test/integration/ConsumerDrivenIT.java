package test.integration;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import static com.github.t1.wunderbar.junit.WunderBarJUnitExecutor.findTestsIn;

public class ConsumerDrivenIT {
    @TestFactory DynamicNode demoClient() {
        return findTestsIn("../client/target/wunder.bar");
    }
}
