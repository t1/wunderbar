package com.github.t1.wunderbar.junit.runner;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JUnit-Jupiter calls `@BeforeEach` and `@AfterEach` methods only once before/after a `@TestFactory`.
 * You can use `@AfterDynamicTest` to clean up the data you need in your service to comply with the requirements.
 * The annotated method optionally gets a <code>List<HttpServerInteraction> interactions</code> parameter,
 * so it can derive the data it has to clean up.
 */
@Retention(RUNTIME)
public @interface AfterDynamicTest {}
