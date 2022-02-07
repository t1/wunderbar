package com.github.t1.wunderbar.junit.consumer;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Generate random data, as {@link org.junit.jupiter.api.extension.ParameterResolver parameter} or as field.
 * <p>
 * TODO document
 */
@Retention(RUNTIME)
public @interface Some {
    /** The generator to use; defaults to a generator for {@link SomeBasics some basic types}. */
    Class<? extends SomeData> of() default SomeBasics.class;
}
