package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JUnit-Jupiter calls {@link org.junit.jupiter.api.BeforeEach BeforeEach} and {@link org.junit.jupiter.api.AfterEach AfterEach}
 * methods only once before/after a {@link org.junit.jupiter.api.TestFactory TestFactory}.
 * You can use this annotation on a method to, e.g., clean up the data you need in your service to comply with the requirements.
 * <p>
 * The annotated method can optionally take parameters of these types:
 * <ul>
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction}&gt;: expected interactions</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest}&gt;: expected requests</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse}&gt;: expected responses</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.provider.ActualHttpResponse ActualHttpResponse}&gt;: actual responses</code>,
 * <li><code>{@link com.github.t1.wunderbar.junit.provider.WunderBarExecutions WunderBarExecutions}</code>: meta data about
 * the running dynamic test</li>
 * </ul>
 * <p>
 * Note: it's sometimes easier to use {@link AfterInteraction}, esp. if you have only a single interaction in a test,
 * you don't need the overhead with the lists.
 *
 * @see AfterInteraction
 */
@Retention(RUNTIME)
public @interface AfterDynamicTest {}
