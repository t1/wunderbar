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
}
