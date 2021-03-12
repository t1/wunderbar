package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * You can use this annotation to set up the data you need in your service to comply with the requirements.
 * <p>
 * The annotated method optionally gets a single
 * {@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction} parameter,
 * so it can derive the data it has to set up.
 * <p>
 * The annotated method can optionally return a {@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction},
 * {@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest}, or a
 * {@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse} to replace the
 * interaction being used. This may be necessary, e.g. to replace the dummy credentials with real ones.
 *
 * @see BeforeDynamicTest
 */
@Retention(RUNTIME)
public @interface BeforeInteraction {}
