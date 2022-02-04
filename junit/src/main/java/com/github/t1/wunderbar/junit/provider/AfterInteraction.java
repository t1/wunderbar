package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * You can use this annotation to clean up the data of a single interaction.
 * The annotated method optionally gets one of these parameters, so it can derive the data it has to clean up:
 * <ul>
 * <li><code>{@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest} request</code>,
 * <li><code>{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse} response</code>,
 * </ul>
 *
 * @see AfterDynamicTest
 */
@Retention(RUNTIME)
public @interface AfterInteraction {}
