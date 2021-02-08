package test.integration;

import com.github.t1.wunderbar.junit.WunderBarExecutorJUnit;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;

public class ConsumerDrivenIT {
    @TestFactory DynamicNode demoClient() {
        return new WunderBarExecutorJUnit(Path.of("../client/target/wunder.bar")).build();
    }
}
