package test.runner;

import com.github.t1.wunderbar.junit.runner.MavenCoordinates;
import com.github.t1.wunderbar.junit.runner.WunderBarRunner;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.Slow;

import static com.github.t1.wunderbar.junit.Utils.deleteRecursive;
import static java.nio.file.Files.exists;

@WunderBarRunner(baseUri = "dummy")
class FindInArtifactIT {
    private static final MavenCoordinates MAVEN_CENTRAL_BAR_ARTIFACT = MavenCoordinates.builder()
        .groupId("com.github.t1")
        .artifactId("wunderbar.demo.order")
        .version("1.0.2")
        .build();

    @RegisterExtension static RunnerFixture fixture = new RunnerFixture();

    @Slow
    @TestFactory DynamicNode artifactDownload() {
        var parent = MAVEN_CENTRAL_BAR_ARTIFACT.getLocalRepositoryPath().getParent();
        if (exists(parent)) deleteRecursive(parent);

        fixture.in(MAVEN_CENTRAL_BAR_ARTIFACT
            .withPackaging("bar").withClassifier("bar") // these are the defaults set in the WunderBarTestFinder
            .getLocalRepositoryPath())
            // this is the exact order in the file
            .expect("ProductsResolverIT/shouldResolveProduct", 1)
            .expect("ProductsResolverIT/shouldFailToResolveForbiddenProduct", 1)
            .expect("ProductsResolverIT/shouldFailToResolveUnknownProduct", 1)
            .expect("ProductsResolverIT/shouldResolveTwoProducts", 2)
            .expect("ProductsGatewayIT/shouldGetTwoProducts", 2)
            .expect("ProductsGatewayIT/shouldFailToGetUnknownProduct", 1)
            .expect("ProductsGatewayIT/shouldFailToGetForbiddenProduct", 1)
            .expect("ProductsGatewayIT/shouldGetProduct", 1);

        return fixture.findTestsInArtifact(MAVEN_CENTRAL_BAR_ARTIFACT);
    }
}
