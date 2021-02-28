package com.github.t1.wunderbar.junit.consumer;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The API interface (<code>RegisterRestClient</code> or <code>GraphQlClientApi</code>)
 * a {@link WunderBarConsumer} test uses for indirect input and output; the mock, generally.
 */
@Retention(RUNTIME)
public @interface Service {}
