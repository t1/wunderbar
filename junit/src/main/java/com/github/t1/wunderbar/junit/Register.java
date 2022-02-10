package com.github.t1.wunderbar.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Register this field as a {@link com.github.t1.wunderbar.junit.consumer.SomeData test data generator}
 * to be usable with {@link com.github.t1.wunderbar.junit.consumer.Some @Some}
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Register {
}
