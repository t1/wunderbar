package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * You can use this annotation to, e.g., check the result of an unexpectedly failing interaction,
 * i.e. an interaction that doesn't behave as specified in the bar. This is mainly useful for
 * testing WunderBar itself; to clean things up, use {@link AfterInteraction}, which is called
 * directly before an {@link OnInteractionError} methods.
 * <p>
 * If there are several methods annotated as {@link OnInteractionError}, their execution order is not defined;
 * but you can use the {@link org.junit.jupiter.api.Order} annotation to specify the order explicitly.
 * <p>
 * The annotated method can take any of these parameters:
 * <ul>
 * <li>{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction}: the expected request and response
 * <li>{@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest}: the expected request
 * <li>{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse}: the expected response
 * <li>{@link com.github.t1.wunderbar.junit.provider.ActualHttpResponse ActualHttpResponse}: the actual response
 * <li>{@link org.assertj.core.api.BDDSoftAssertions BDDSoftAssertions}: the mismatches that WunderBar has found. You can turn them
 *     into an {@link AssertionError} by calling {@link org.assertj.core.api.BDDSoftAssertions#assertAll() assertAll()}.
 * <li><code>{@link com.github.t1.wunderbar.junit.provider.WunderBarExecution WunderBarExecution}</code>: meta data about
 * the running dynamic test</li>
 * </ul>
 *
 * @see AfterInteraction
 */
@Retention(RUNTIME)
public @interface OnInteractionError {}
