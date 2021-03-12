package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * You can use this annotation to clean up the data of a single interaction.
 * The annotated method optionally gets a
 * <code>{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction} interaction</code>
 * parameter, so it can derive the data it has to set up.
 *
 * @see AfterDynamicTest
 */
@Retention(RUNTIME)
public @interface AfterInteraction {}
