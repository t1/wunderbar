package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * You can use this annotation to set up the data you need in your service to comply with the requirements.
 * The annotated method optionally gets a single <code>HttpServerInteraction interaction</code> parameter,
 * so it can derive the data it has to set up.
 *
 * @see BeforeDynamicTest
 */
@Retention(RUNTIME)
public @interface BeforeInteraction {}
