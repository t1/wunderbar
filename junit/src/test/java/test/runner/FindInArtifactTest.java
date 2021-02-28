package test.runner;

import com.github.t1.wunderbar.junit.runner.MavenCoordinates;
import com.github.t1.wunderbar.junit.runner.WunderBarRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.t1.wunderbar.junit.Utils.deleteRecursive;

@WunderBarRunner(baseUri = "dummy")
class FindInArtifactTest {
    @RegisterExtension static RunnerFixture fixture = new RunnerFixture();

    Path tmpDir;
    Path versionDir;

    private final MavenCoordinates COORDINATES = MavenCoordinates.of("com.github.t1:wunderbar.test.artifact:1.2.3");

    @BeforeEach void setUp() throws IOException {
        tmpDir = Path.of(System.getProperty("user.home")).resolve(".m2/repository/com/github/t1/wunderbar.test.artifact");
        versionDir = tmpDir.resolve("1.2.3");
        Files.createDirectories(versionDir);
    }

    @AfterEach
    void tearDown() { deleteRecursive(tmpDir); }

    @TestFactory DynamicNode artifactTestWithSpecifiedClassifierAndPackaging() {
        fixture.in(versionDir.resolve("wunderbar.test.artifact-1.2.3-bar.jar"))
            .withTest("artifact-test")

            .expect("artifact-test", 1);

        return fixture.findTestsInArtifact(COORDINATES.withPackaging("jar").withClassifier("bar"));
    }

    @TestFactory DynamicNode artifactTestWithDefaultClassifierAndSpecifiedPackaging() {
        fixture.in(versionDir.resolve("wunderbar.test.artifact-1.2.3-bar.jar"))
            .withTest("artifact-test")

            .expect("artifact-test", 1);

        return fixture.findTestsInArtifact(COORDINATES.withPackaging("jar"));
    }

    @TestFactory DynamicNode artifactTestWithSpecifiedClassifierAndDefaultPackaging() {
        fixture.in(versionDir.resolve("wunderbar.test.artifact-1.2.3-foo.bar"))
            .withTest("artifact-test")

            .expect("artifact-test", 1);

        return fixture.findTestsInArtifact(COORDINATES.withClassifier("foo"));
    }

    @TestFactory DynamicNode artifactWithDefaultClassifierAndPackagingTest() {
        fixture.in(versionDir.resolve("wunderbar.test.artifact-1.2.3-bar.bar"))
            .withTest("artifact-test")

            .expect("artifact-test", 1);

        return fixture.findTestsInArtifact(COORDINATES);
    }
}
