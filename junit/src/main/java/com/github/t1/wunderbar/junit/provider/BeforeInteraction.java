package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * You can use this annotation on a method to, e.g., set up the data you need in your service to comply with this interaction.
 * <p>
 * The annotated method can optionally take parameters of these types:
 * <ul>
 * <li><code>{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction}: expected interactions</code>,
 * <li><code>{@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest}: expected requests</code>,
 * <li><code>{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse}: expected responses</code>,
 * <li><code>{@link com.github.t1.wunderbar.junit.provider.WunderBarExecution WunderBarExecution}</code>: meta data about
 * the running interaction</li>
 * </ul>
 * <p>
 * The annotated method can optionally return an object of one of these types to change the expectations,
 * e.g. to replace the dummy credentials with real ones:
 * <ul>
 * <li><code>{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction}: expected interactions</code>,
 * <li><code>{@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest}: expected requests</code>,
 * <li><code>{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse}: expected responses</code>,
 * </ul>
 *
 * @see BeforeDynamicTest
 * @see AfterInteraction
 */
@Retention(RUNTIME)
public @interface BeforeInteraction {}
