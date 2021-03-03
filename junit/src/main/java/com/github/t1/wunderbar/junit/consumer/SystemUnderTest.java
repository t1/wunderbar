package com.github.t1.wunderbar.junit.consumer;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The consumer code that has a field (generally annotated as <code>Inject</code>) with an API interface
 * (<code>RegisterRestClient</code> or <code>GraphQlClientApi</code>) and that a {@link WunderBarApiConsumer} test
 * tests. In some rare cases it may make sense to test only the the API interface and the DTOs, and not the code that
 * actually consumes that API, so it's not strictly necessary to declare a {@link SystemUnderTest}.
 */
@Retention(RUNTIME)
public @interface SystemUnderTest {}
