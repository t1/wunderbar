package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JUnit-Jupiter calls {@link org.junit.jupiter.api.BeforeEach BeforeEach} and {@link org.junit.jupiter.api.AfterEach AfterEach}
 * methods only once before/after a {@link org.junit.jupiter.api.TestFactory TestFactory}.
 * You can use this annotation on a method to, e.g., set up <em>all the data</em> you need in your service to comply with the test.
 * <p>
 * The annotated method can optionally take parameters of these types:
 * <ul>
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction}&gt;: expected interactions</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest}&gt;: expected requests</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse}&gt;: expected responses</code>,
 * <li><code>{@link com.github.t1.wunderbar.junit.provider.WunderBarExecutions WunderBarExecutions}</code>: meta data about
 * the running dynamic test</li>
 * </ul>
 * <p>
 * The annotated method can optionally return an object of one of these types to change the expectations,
 * e.g. to replace the dummy credentials with real ones:
 * <ul>
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction}&gt;: expected interactions</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest}&gt;: expected requests</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse}&gt;: expected responses</code>,
 * </ul>
 * <p>
 * Note: it's generally easier to use {@link BeforeInteraction}, esp. if you have interactions that mutate the result of previous interactions.
 * <p>
 * You can actually add or remove expected HttpInteractions!
 *
 * @see BeforeInteraction
 */
@Retention(RUNTIME)
public @interface BeforeDynamicTest {}
