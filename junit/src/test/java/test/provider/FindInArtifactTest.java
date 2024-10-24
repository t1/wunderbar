package test.provider;

import com.github.t1.wunderbar.junit.provider.MavenCoordinates;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.NonCI;
import test.Slow;

import java.io.IOException;
import java.nio.file.Path;

import static com.github.t1.wunderbar.common.Utils.deleteRecursive;
import static java.nio.file.Files.createDirectories;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiProvider(baseUri = "dummy")
class FindInArtifactTest {
    @RegisterExtension static ApiProviderFixture fixture = new ApiProviderFixture();

    Path tmpDir;
    Path versionDir;

    private final MavenCoordinates COORDINATES = MavenCoordinates.of("com.github.t1:wunderbar.test.artifact:1.2.3");

    @BeforeEach void setUp() throws IOException {
        tmpDir = Path.of(System.getProperty("user.home")).resolve(".m2/repository/com/github/t1/wunderbar.test.artifact");
        versionDir = tmpDir.resolve("1.2.3");
        createDirectories(versionDir);
    }

    @AfterEach
    void tearDown() {
        deleteRecursive(tmpDir);
    }

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

    @Test void shouldFailToParseEmptyCoordinates() {
        var throwable = catchThrowable(() -> MavenCoordinates.of(""));

        then(throwable).hasMessage("invalid Maven coordinates []");
    }

    @Test void shouldParseFullCoordinates() {
        var string = "com.github.t1:wunderbar.test.artifact:1.2.3:jar:bar";

        var coordinates = MavenCoordinates.of(string);

        then(coordinates.getCompactString()).isEqualTo(string);
        then(coordinates.getGroupId()).isEqualTo("com.github.t1");
        then(coordinates.getArtifactId()).isEqualTo("wunderbar.test.artifact");
        then(coordinates.getVersion()).isEqualTo("1.2.3");
        then(coordinates.getPackaging()).isEqualTo("jar");
        then(coordinates.getClassifier()).isEqualTo("bar");
        then(coordinates).hasToString(
                "MavenCoordinates(groupId=com.github.t1, artifactId=wunderbar.test.artifact, " +
                "version=1.2.3, packaging=jar, classifier=bar)");
    }

    @Slow @NonCI
    @Test void shouldFailToDownloadMissingCoordinates() {
        var missingCoordinates = COORDINATES.withVersion("0.0.0");

        var throwable = catchThrowable(missingCoordinates::download);

        then(throwable).hasMessage("can't download maven artifact: " + missingCoordinates.getCompactString());
    }
}
