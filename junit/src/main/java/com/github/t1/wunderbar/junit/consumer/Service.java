package com.github.t1.wunderbar.junit.consumer;

import jakarta.enterprise.util.AnnotationLiteral;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// The API interface ([RegisterRestClient][org.eclipse.microprofile.rest.client.inject.RegisterRestClient]
/// or [GraphQLClientApi][io.smallrye.graphql.client.typesafe.api.GraphQLClientApi])
/// a [WunderBarApiConsumer] test uses for indirect input and output; the mock, generally speaking.
///
/// A `@Service` will be injected into the [SystemUnderTest].
@Retention(RUNTIME)
public @interface Service {
    /// Base uri template where the service runs. Defaults to `http://localhost:{port}/{technology}`.
    ///
    /// Supported template expressions:
    /// - A method template variable like `{foo()}` will be replaced by the result of a call to the (maybe static) method
    ///   of that name in the test class.
    /// - The template variable `technology` will be replaced by `graphql` or `rest` respectively.
    /// - The `{port}` will be replaced by the [port] property, i.e. by default `RANDOM`.
    ///
    /// Note that the replacement happens in exactly this order, i.e. you can return `{technology}` from your
    /// `{endpoint()}` function, and it will be replaced properly.
    String endpoint() default DEFAULT_ENDPOINT;

    String DEFAULT_ENDPOINT = "http://localhost:{port}/{technology}";

    /// Port number of the service. Defaults to `RANDOM` (zero), i.e. a random, unused port.
    /// Will be ignored for [UNIT][Level.UNIT] level tests.
    int port() default RANDOM;

    /// Indicates that the HTTP server of an integration test should run on a random, free port.
    int RANDOM = 0;

    @Getter @Accessors(fluent = true) @With @RequiredArgsConstructor
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Literal extends AnnotationLiteral<Service> implements Service {
        private final int port;
        private final String endpoint;
    }

    Service.Literal DEFAULT = new Service.Literal(RANDOM, DEFAULT_ENDPOINT);
}
