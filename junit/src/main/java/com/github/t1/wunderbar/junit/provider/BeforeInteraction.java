package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * You can use this annotation to set up the data you need in your service to comply with the requirements.
 * <p>
 * The annotated method optionally gets a single
 * <code>{@link com.github.t1.wunderbar.junit.http.HttpServerInteraction HttpServerInteraction} interaction</code> parameter,
 * so it can derive the data it has to set up.
 * <p>
 * The annotated method can optionally return a <code>{@link com.github.t1.wunderbar.junit.http.HttpServerInteraction HttpServerInteraction}</code>,
 * <code>{@link com.github.t1.wunderbar.junit.http.HttpServerRequest HttpServerRequest}</code>, or a
 * <code>{@link com.github.t1.wunderbar.junit.http.HttpServerResponse HttpServerResponse}</code> to replace the
 * interaction being used. This may be necessary, e.g. to replace the dummy credentials with real ones.
 *
 * @see BeforeDynamicTest
 */
@Retention(RUNTIME)
public @interface BeforeInteraction {}
