package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JUnit-Jupiter calls {@link org.junit.jupiter.api.BeforeEach BeforeEach} and {@link org.junit.jupiter.api.AfterEach AfterEach}
 * methods only once before/after a {@link org.junit.jupiter.api.TestFactory TestFactory}, i.e. once per <code>bar</code> file.
 * In contrast, WunderBar runs methods annotated as {@link BeforeDynamicTest} before every test within a <code>bar</code> file
 * (and {@link BeforeInteraction} methods before every interaction within one test; see there for details).
 * You can use this annotation on a method to, e.g., set up <em>all the data</em> you need in your service to comply with the test.
 * <p>
 * If there are several methods annotated as {@link BeforeDynamicTest}, their execution order is not defined;
 * but you can use the {@link org.junit.jupiter.api.Order} annotation to specify the order explicitly.
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
 * These lists are immutable; in order to change the interactions, the annotated method can optionally return an object
 * of one of these types. You can use this to, e.g., replace the dummy credentials with real ones:
 * <ul>
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpInteraction HttpInteraction}&gt;: expected interactions</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpRequest HttpRequest}&gt;: expected requests</code>,
 * <li><code>List&lt;{@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse}&gt;: expected responses</code>,
 * </ul>
 * <p>
 * Note: it's generally easier to use {@link BeforeInteraction}, esp. if you have interactions that mutate the result of previous interactions.
 * <p>
 * You can actually add or remove expected HttpInteractions, e.g., to filter the tests that actually need to run.
 *
 * @see BeforeInteraction
 */
@Retention(RUNTIME)
public @interface BeforeDynamicTest {}
