package com.github.t1.wunderbar.junit.consumer;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The consumer code that has a field (generally annotated as <code>{@link javax.inject.Inject @Inject}</code>)
 * with an API interface ({@link org.eclipse.microprofile.rest.client.inject.RegisterRestClient RegisterRestClient} or
 * {@link io.smallrye.graphql.client.typesafe.api.GraphQLClientApi GraphQLClientApi})
 * and that a {@link WunderBarApiConsumer} test tests.
 * In some cases it may make sense to test only the the API interface and the DTOs, and not the code that
 * actually consumes that API, so it's not necessary to declare a {@link SystemUnderTest}. Or you could have several.
 */
@Retention(RUNTIME)
public @interface SystemUnderTest {}
