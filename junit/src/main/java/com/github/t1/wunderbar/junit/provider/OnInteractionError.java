package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * You can use this annotation to check the result of an unexpectedly failing interaction,
 * i.e. an interaction that doesn't behave as specified in the bar. This is mainly useful for
 * testing WunderBar itself; to clean things up, use {@link AfterInteraction}, which is called
 * directly before an {@link OnInteractionError}.
 * <p>
 * The annotated method can take any of these parameters:
 * <ul>
 * <li>{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction}: the expected request and response
 * <li>{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse}: the actual response
 * <li>{@link org.assertj.core.api.BDDSoftAssertions BDDSoftAssertions}: the mismatches that WunderBar has found. You can turn them
 *     into an {@link AssertionError} by calling {@link org.assertj.core.api.BDDSoftAssertions#assertAll() assertAll()}.
 * </ul>
 *
 * @see AfterInteraction
 */
@Retention(RUNTIME)
public @interface OnInteractionError {}
