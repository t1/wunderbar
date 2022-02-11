package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.consumer.SomeData;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Register these classes as {@link com.github.t1.wunderbar.junit.consumer.SomeData test data generators}
 * to be usable with {@link com.github.t1.wunderbar.junit.consumer.Some @Some}
 */
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Register {
    Class<? extends SomeData>[] value() default {};
}
