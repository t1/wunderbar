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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.junit.consumer.Level.INTEGRATION;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static org.assertj.core.api.BDDAssertions.then;

class GraphQlAuthorizationIT {
    private static final List<Runnable> AFTER_ALL_CHECKS = new ArrayList<>();
    private static final String UNAUTHORIZED_GRAPHQL_REQUEST_HEADERS =
        "Method: POST\n" +
        "URI: /graphql\n" +
        "Accept: " + APPLICATION_JSON_UTF8 + "\n" +
        "Content-Type: " + APPLICATION_JSON_UTF8 + "\n";
    private static final String AUTHORIZED_GRAPHQL_REQUEST_HEADERS =
        UNAUTHORIZED_GRAPHQL_REQUEST_HEADERS +
        "Authorization: Dummy authorization\n";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String DUMMY_CREDENTIALS = "Basic ZHVtbXktdXNlcm5hbWU6ZHVtbXktcGFzc3dvcmQ=";  // dummy-username:dummy-password
    private static final String DUMMY_TOKEN = "Bearer dummy-token";


    @AfterAll static void afterAll() {
        AFTER_ALL_CHECKS.forEach(Runnable::run);
    }


    @GraphQLClientApi
    interface NonAuthorizedMethodProducts {
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = "target/NoCredentialsGenerator-bar/")
    @Nested class NoCredentialsGenerator {
        @Service NonAuthorizedMethodProducts nonAuthorizedMethodProducts;

        @Test void shouldCallWithoutCredentials() {
            var givenProduct = Product.builder().id("nam").build();
            given(nonAuthorizedMethodProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = nonAuthorizedMethodProducts.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(writtenHeader("shouldCallWithoutCredentials", "NoCredentialsGenerator"))
                .hasContent(UNAUTHORIZED_GRAPHQL_REQUEST_HEADERS);
        }

    }


    @GraphQLClientApi
    interface NonAuthorizedMethodProducts2 {
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = "target/SystemPropertyCredentialsGenerator-bar/")
    @Nested class SystemPropertyCredentialsGenerator {
        @Service NonAuthorizedMethodProducts2 nonAuthorizedMethodProducts;

        @Test void shouldGenerateAndRestoreCredentialsProperties() {
            var propName = NonAuthorizedMethodProducts2.class.getName() + "/mp-graphql/";
            System.setProperty(propName + "username", "original-username");
            System.setProperty(propName + "password", "original-password");
            var givenProduct = Product.builder().id("nam").build();
            given(nonAuthorizedMethodProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = nonAuthorizedMethodProducts.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(writtenHeader("shouldGenerateAndRestoreCredentialsProperties", "SystemPropertyCredentialsGenerator"))
                .hasContent(AUTHORIZED_GRAPHQL_REQUEST_HEADERS);
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

    @WunderBarApiConsumer(level = INTEGRATION, fileName = "target/MethodCredentialsGenerator-bar/")
    @Nested class MethodCredentialsGenerator {
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
            then(writtenHeader("shouldGenerateAndRestoreCredentials", "MethodCredentialsGenerator"))
                .hasContent(AUTHORIZED_GRAPHQL_REQUEST_HEADERS);
            then(System.getProperty(propName + "username")).isEqualTo("dummy-username");
            then(System.getProperty(propName + "password")).isEqualTo("dummy-password");
            AFTER_ALL_CHECKS.add(() -> {
                then(System.getProperty(propName + "username")).describedAs("shouldGenerateAndRestoreCredentials")
                    .isEqualTo("original-username");
                then(System.getProperty(propName + "password")).describedAs("shouldGenerateAndRestoreCredentials")
                    .isEqualTo("original-password");
            });
        }

        @Test void shouldGenerateAndRestoreNullCredentials() {
            var propName = AuthorizedMethodProducts2.class.getName() + "/mp-graphql/";
            var givenProduct = Product.builder().id("am2").build();
            given(authorizedMethodProducts2.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = authorizedMethodProducts2.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(writtenHeader("shouldGenerateAndRestoreNullCredentials", "MethodCredentialsGenerator"))
                .hasContent(AUTHORIZED_GRAPHQL_REQUEST_HEADERS);
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

    @WunderBarApiConsumer(level = INTEGRATION, fileName = "target/InterfaceCredentialsGenerator-bar/")
    @Nested class InterfaceCredentialsGenerator {
        @Service AuthorizedInterfaceProducts authorizedInterfaceProducts;

        @Test void shouldGenerateAndRestoreInterfaceCredentials() {
            var propName = AuthorizedInterfaceProducts.class.getName() + "/mp-graphql/";
            System.setProperty(propName + "username", "original-username");
            System.setProperty(propName + "password", "original-password");
            var givenProduct = Product.builder().id("am").build();
            given(authorizedInterfaceProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = authorizedInterfaceProducts.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(writtenHeader("shouldGenerateAndRestoreInterfaceCredentials", "InterfaceCredentialsGenerator"))
                .hasContent(AUTHORIZED_GRAPHQL_REQUEST_HEADERS);
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

    @WunderBarApiConsumer(level = INTEGRATION, fileName = "target/InterfaceHeaderCredentialsGenerator-bar/")
    @Nested class InterfaceHeaderCredentialsGenerator {
        @Service AuthorizedInterfaceHeaderProducts products;

        @Test void shouldGenerateAndRestoreInterfaceHeaderCredentials() {
            var givenProduct = Product.builder().id("am").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = products.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(writtenHeader("shouldGenerateAndRestoreInterfaceHeaderCredentials", "InterfaceHeaderCredentialsGenerator"))
                .hasContent(AUTHORIZED_GRAPHQL_REQUEST_HEADERS);
        }
    }


    @GraphQLClientApi
    interface AuthorizedParameterHeaderProducts {
        Product product(@Header(name = "Authorization") String auth, String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = "target/ParameterHeaderCredentialsGenerator-bar/")
    @Nested class ParameterHeaderCredentialsGenerator {
        @Service AuthorizedParameterHeaderProducts products;

        @Test void shouldGenerateAndRestoreParameterHeaderCredentials() {
            var givenProduct = Product.builder().id("am").build();
            given(products.product(DUMMY_CREDENTIALS, givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = products.product(DUMMY_CREDENTIALS, givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(writtenHeader("shouldGenerateAndRestoreParameterHeaderCredentials", "ParameterHeaderCredentialsGenerator"))
                .hasContent(AUTHORIZED_GRAPHQL_REQUEST_HEADERS);
        }
    }


    @GraphQLClientApi
    @Header(name = "Authorization", constant = DUMMY_TOKEN)
    interface AuthorizedInterfaceTokenHeaderProducts {
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = "target/InterfaceTokenHeaderCredentialsGenerator-bar/")
    @Nested class InterfaceTokenHeaderCredentialsGenerator {
        @Service AuthorizedInterfaceTokenHeaderProducts products;

        @Test void shouldGenerateAndRestoreInterfaceTokenHeaderCredentials() {
            var givenProduct = Product.builder().id("am").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = products.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(writtenHeader("shouldGenerateAndRestoreInterfaceTokenHeaderCredentials", "InterfaceTokenHeaderCredentialsGenerator"))
                .hasContent(AUTHORIZED_GRAPHQL_REQUEST_HEADERS);
        }
    }

    private Path writtenHeader(String methodName, String nestedClassName) {
        return TestBackdoor.writtenBar("target/" + nestedClassName + "-bar/").resolve(
            "GraphQlAuthorizationIT/" + nestedClassName + "/" +
            methodName + "/1 request-headers.properties");
    }
}
