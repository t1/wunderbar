package com.github.t1.wunderbar.junit.consumer;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The API interface ({@link org.eclipse.microprofile.rest.client.inject.RegisterRestClient RegisterRestClient}
 * or {@link io.smallrye.graphql.client.typesafe.api.GraphQLClientApi GraphQLClientApi})
 * a {@link WunderBarApiConsumer} test uses for indirect input and output; the mock, generally.
 */
@Retention(RUNTIME)
public @interface Service {
    /**
     * Port number of the mock http server starting for {@link Level#INTEGRATION INTEGRATION} level test.
     * Will be ignored for non-{@link Level#INTEGRATION INTEGRATION} level tests.
     */
    int port() default RANDOM;

    /** Indicates that the http server of an integration test should run on a random, free port. */
    int RANDOM = 0;
}
