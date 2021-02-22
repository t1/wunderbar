package com.github.t1.wunderbar.junit.runner;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JUnit-Jupiter calls {@link org.junit.jupiter.api.BeforeEach BeforeEach} and {@link org.junit.jupiter.api.AfterEach AfterEach}
 * methods only once before/after a {@link org.junit.jupiter.api.TestFactory TestFactory}.
 * You can use this annotation to set up the data you need in your service to comply with the requirements.
 * The annotated method optionally gets a <code>List&lt;HttpServerInteraction&gt; interactions</code> parameter,
 * so it can derive the data it has to set up.
 */
@Retention(RUNTIME)
public @interface BeforeDynamicTest {}
