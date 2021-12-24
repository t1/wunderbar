package com.github.t1.wunderbar.junit.consumer;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The {@link WunderBarApiConsumer} extension will do basic dependency injection for fields annotated as SUT, namely
 * inject those beans it has created for the fields annotated as {@link Service}.
 * If you need more sophisticated dependency injection, i.e. other types, deeply nested classes, interceptors, etc.,
 * you can use a full dependency injection (test) framework, e.g., <code>weld-junit5</code>.
 * For more details see the <a href="https://github.com/t1/wunderbar#full-dependency-injection">README</a>.
 */
@Retention(RUNTIME)
public @interface SystemUnderTest {}
