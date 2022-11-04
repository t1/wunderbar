package com.github.t1.wunderbar.junit.consumer;

import jakarta.enterprise.util.AnnotationLiteral;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The API interface ({@link org.eclipse.microprofile.rest.client.inject.RegisterRestClient RegisterRestClient}
 * or {@link io.smallrye.graphql.client.typesafe.api.GraphQLClientApi GraphQLClientApi})
 * a {@link WunderBarApiConsumer} test uses for indirect input and output; the mock, generally speaking.
 * <p>
 * A <code>@Service</code> will be injected into the {@link SystemUnderTest}.
 */
@Retention(RUNTIME)
public @interface Service {
    /**
     * Base uri template where the service runs. Defaults to <code>{@value DEFAULT_ENDPOINT}</code>.
     * <p>
     * Supported template expressions:
     * <ul>
     * <li>A method template variable like <code>{foo()}</code> will be replaced by the result of a call to the (maybe static) method
     *     of that name in the test class.
     * <li>The template variable <code>technology</code> will be replaced by <code>graphql</code> or <code>rest</code> respectively.
     * <li>The <code>{port}</code> will be replaced by the {@link #port()} property, i.e. by default <code>RANDOM</code>.
     * </ul>
     * <p>
     * Note that the replacement happens in exactly this order, i.e. you can return <code>{technology}</code> from your
     * <code>{endpoint()}</code> function, and it will be replaced properly.
     */
    String endpoint() default DEFAULT_ENDPOINT;

    String DEFAULT_ENDPOINT = "http://localhost:{port}/{technology}";

    /**
     * Port number of the service. Defaults to <code>RANDOM</code> (zero), i.e. a random, unused port.
     * Will be ignored for {@link Level#UNIT UNIT} level tests.
     */
    int port() default RANDOM;

    /** Indicates that the http server of an integration test should run on a random, free port. */
    int RANDOM = 0;

    @Getter @Accessors(fluent = true) @With @RequiredArgsConstructor
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Literal extends AnnotationLiteral<Service> implements Service {
        private final int port;
        private final String endpoint;
    }

    Service.Literal DEFAULT = new Service.Literal(RANDOM, DEFAULT_ENDPOINT);
}
