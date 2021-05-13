package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.TestBackdoor;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.Header;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Product;

import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.junit.consumer.Level.INTEGRATION;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.BDDAssertions.then;
import static test.consumer.GraphQlAuthorizationIT.InterfaceCredentialsGenerator.INTERFACE_CREDENTIALS_BAR;
import static test.consumer.GraphQlAuthorizationIT.InterfaceHeaderCredentialsGenerator.INTERFACE_HEADER_CREDENTIALS_BAR;
import static test.consumer.GraphQlAuthorizationIT.InterfaceTokenHeaderCredentialsGenerator.INTERFACE_TOKEN_HEADER_CREDENTIALS_BAR;
import static test.consumer.GraphQlAuthorizationIT.MethodCredentialsGenerator.METHOD_CREDENTIALS_BAR;
import static test.consumer.GraphQlAuthorizationIT.NoCredentialsGenerator.NO_CREDENTIALS_BAR;
import static test.consumer.GraphQlAuthorizationIT.ParameterHeaderCredentialsGenerator.PARAMETER_HEADER_CREDENTIALS_BAR;

public class GraphQlAuthorizationIT {
    private static final List<Runnable> AFTER_ALL_CHECKS = new ArrayList<>();
    private static final String UNAUTHORIZED_GRAPHQL = "" +
        "Method: POST\n" +
        "URI: /graphql\n" +
        "Content-Type: application/json;charset=utf-8\n";
    private static final String AUTHORIZED_GRAPHQL_REQUEST = UNAUTHORIZED_GRAPHQL +
        "Authorization: Dummy authorization\n";
    private static final String DUMMY_CREDENTIALS = "Basic ZHVtbXktdXNlcm5hbWU6ZHVtbXktcGFzc3dvcmQ=";  // dummy-username:dummy-password
    private static final String DUMMY_TOKEN = "Bearer dummy-token";


    @GraphQLClientApi
    interface NonAuthorizedMethodProducts {
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = NO_CREDENTIALS_BAR)
    @Nested class NoCredentialsGenerator {
        static final String NO_CREDENTIALS_BAR = "target/NoCredentialsGenerator-bar/";

        @Service NonAuthorizedMethodProducts nonAuthorizedMethodProducts;

        @Test void shouldGenerateAndRestoreCredentials() {
            var propName = NonAuthorizedMethodProducts.class.getName() + "/mp-graphql/";
            System.setProperty(propName + "username", "original-username");
            System.setProperty(propName + "password", "original-password");
            var givenProduct = Product.builder().id("nam").build();
            given(nonAuthorizedMethodProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = nonAuthorizedMethodProducts.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(TestBackdoor.writtenBar(NO_CREDENTIALS_BAR).resolve("GraphQlAuthorizationIT/NoCredentialsGenerator/" +
                "shouldGenerateAndRestoreCredentials/1 request-headers.properties")).hasContent(UNAUTHORIZED_GRAPHQL);
            then(System.getProperty(propName + "username")).isEqualTo("original-username"); // not changed
            then(System.getProperty(propName + "password")).isEqualTo("original-password"); // not changed
        }
    }

    @GraphQLClientApi
    interface AuthorizedMethodProducts {
        @AuthorizationHeader
        Product product(String id);
    }

    @GraphQLClientApi
    interface AuthorizedMethodProducts2 {
        @AuthorizationHeader
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = METHOD_CREDENTIALS_BAR)
    @Nested class MethodCredentialsGenerator {
        static final String METHOD_CREDENTIALS_BAR = "target/MethodCredentialsGenerator-bar/";
        @Service AuthorizedMethodProducts authorizedMethodProducts;
        @Service AuthorizedMethodProducts2 authorizedMethodProducts2;

        @Test void shouldGenerateAndRestoreCredentials() {
            var propName = AuthorizedMethodProducts.class.getName() + "/mp-graphql/";
            System.setProperty(propName + "username", "original-username");
            System.setProperty(propName + "password", "original-password");
            var givenProduct = Product.builder().id("am").build();
            given(authorizedMethodProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = authorizedMethodProducts.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(TestBackdoor.writtenBar(METHOD_CREDENTIALS_BAR).resolve("GraphQlAuthorizationIT/MethodCredentialsGenerator/" +
                "shouldGenerateAndRestoreCredentials/1 request-headers.properties")).hasContent(AUTHORIZED_GRAPHQL_REQUEST);
            then(System.getProperty(propName + "username")).isEqualTo("dummy-username");
            then(System.getProperty(propName + "password")).isEqualTo("dummy-password");
            AFTER_ALL_CHECKS.add(() -> {
                then(System.getProperty(propName + "username")).describedAs("shouldGenerateAndRestoreCredentials").isEqualTo("original-username");
                then(System.getProperty(propName + "password")).describedAs("shouldGenerateAndRestoreCredentials").isEqualTo("original-password");
            });
        }

        @Test void shouldGenerateAndRestoreNullCredentials() {
            var propName = AuthorizedMethodProducts2.class.getName() + "/mp-graphql/";
            var givenProduct = Product.builder().id("am2").build();
            given(authorizedMethodProducts2.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = authorizedMethodProducts2.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(TestBackdoor.writtenBar(METHOD_CREDENTIALS_BAR).resolve("GraphQlAuthorizationIT/MethodCredentialsGenerator/" +
                "shouldGenerateAndRestoreNullCredentials/1 request-headers.properties")).hasContent(AUTHORIZED_GRAPHQL_REQUEST);
            then(System.getProperty(propName + "username")).isEqualTo("dummy-username");
            then(System.getProperty(propName + "password")).isEqualTo("dummy-password");
            AFTER_ALL_CHECKS.add(() -> {
                then(System.getProperty(propName + "username", null))
                    .describedAs("shouldGenerateAndRestoreNullCredentials").isNull();
                then(System.getProperty(propName + "password", null))
                    .describedAs("shouldGenerateAndRestoreNullCredentials").isNull();
            });
        }
    }

    @GraphQLClientApi
    @AuthorizationHeader
    interface AuthorizedInterfaceProducts {
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = INTERFACE_CREDENTIALS_BAR)
    @Nested class InterfaceCredentialsGenerator {
        static final String INTERFACE_CREDENTIALS_BAR = "target/InterfaceCredentialsGenerator-bar/";
        @Service AuthorizedInterfaceProducts authorizedInterfaceProducts;

        @Test void shouldGenerateAndRestoreInterfaceCredentials() {
            var propName = AuthorizedInterfaceProducts.class.getName() + "/mp-graphql/";
            System.setProperty(propName + "username", "original-username");
            System.setProperty(propName + "password", "original-password");
            var givenProduct = Product.builder().id("am").build();
            given(authorizedInterfaceProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = authorizedInterfaceProducts.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(TestBackdoor.writtenBar(INTERFACE_CREDENTIALS_BAR).resolve("GraphQlAuthorizationIT/InterfaceCredentialsGenerator/" +
                "shouldGenerateAndRestoreInterfaceCredentials/1 request-headers.properties")).hasContent(AUTHORIZED_GRAPHQL_REQUEST);
            then(System.getProperty(propName + "username")).isEqualTo("dummy-username");
            then(System.getProperty(propName + "password")).isEqualTo("dummy-password");
            AFTER_ALL_CHECKS.add(() -> {
                then(System.getProperty(propName + "username")).describedAs("shouldGenerateAndRestoreInterfaceCredentials")
                    .isEqualTo("original-username");
                then(System.getProperty(propName + "password")).describedAs("shouldGenerateAndRestoreInterfaceCredentials")
                    .isEqualTo("original-password");
            });
        }
    }


    @GraphQLClientApi
    @Header(name = "Authorization", constant = DUMMY_CREDENTIALS)
    interface AuthorizedInterfaceHeaderProducts {
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = INTERFACE_HEADER_CREDENTIALS_BAR)
    @Nested class InterfaceHeaderCredentialsGenerator {
        static final String INTERFACE_HEADER_CREDENTIALS_BAR = "target/InterfaceHeaderCredentialsGenerator-bar/";
        @Service AuthorizedInterfaceHeaderProducts products;

        @Test void shouldGenerateAndRestoreInterfaceHeaderCredentials() {
            var givenProduct = Product.builder().id("am").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = products.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(TestBackdoor.writtenBar(INTERFACE_HEADER_CREDENTIALS_BAR).resolve("GraphQlAuthorizationIT/InterfaceHeaderCredentialsGenerator/" +
                "shouldGenerateAndRestoreInterfaceHeaderCredentials/1 request-headers.properties")).hasContent(AUTHORIZED_GRAPHQL_REQUEST);
        }
    }


    @GraphQLClientApi
    interface AuthorizedParameterHeaderProducts {
        Product product(@Header(name = "Authorization") String auth, String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = PARAMETER_HEADER_CREDENTIALS_BAR)
    @Nested class ParameterHeaderCredentialsGenerator {
        static final String PARAMETER_HEADER_CREDENTIALS_BAR = "target/ParameterHeaderCredentialsGenerator-bar/";
        @Service AuthorizedParameterHeaderProducts products;

        @Test void shouldGenerateAndRestoreParameterHeaderCredentials() {
            var givenProduct = Product.builder().id("am").build();
            given(products.product(DUMMY_CREDENTIALS, givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = products.product(DUMMY_CREDENTIALS, givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(TestBackdoor.writtenBar(PARAMETER_HEADER_CREDENTIALS_BAR).resolve("GraphQlAuthorizationIT/ParameterHeaderCredentialsGenerator/" +
                "shouldGenerateAndRestoreParameterHeaderCredentials/1 request-headers.properties")).hasContent(AUTHORIZED_GRAPHQL_REQUEST);
        }
    }


    @GraphQLClientApi
    @Header(name = "Authorization", constant = DUMMY_TOKEN)
    interface AuthorizedInterfaceTokenHeaderProducts {
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = INTERFACE_TOKEN_HEADER_CREDENTIALS_BAR)
    @Nested class InterfaceTokenHeaderCredentialsGenerator {
        static final String INTERFACE_TOKEN_HEADER_CREDENTIALS_BAR = "target/InterfaceTokenHeaderCredentialsGenerator-bar/";
        @Service AuthorizedInterfaceTokenHeaderProducts products;

        @Test void shouldGenerateAndRestoreInterfaceTokenHeaderCredentials() {
            var givenProduct = Product.builder().id("am").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = products.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(TestBackdoor.writtenBar(INTERFACE_TOKEN_HEADER_CREDENTIALS_BAR).resolve("GraphQlAuthorizationIT/InterfaceTokenHeaderCredentialsGenerator/" +
                "shouldGenerateAndRestoreInterfaceTokenHeaderCredentials/1 request-headers.properties")).hasContent(AUTHORIZED_GRAPHQL_REQUEST);
        }
    }

    @AfterAll static void afterAll() {
        AFTER_ALL_CHECKS.forEach(Runnable::run);
    }
}
