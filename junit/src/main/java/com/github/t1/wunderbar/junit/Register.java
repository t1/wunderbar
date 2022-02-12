package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.consumer.SomeData;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Register these classes as {@link com.github.t1.wunderbar.junit.consumer.SomeData test data generators}
 * to be usable with {@link com.github.t1.wunderbar.junit.consumer.Some @Some}.
 * <p>
 * Note that you can put this annotation on {@link org.junit.jupiter.api.Nested nested} test classes as well as test methods,
 * and they will only be used in that scope.
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Register {
    Class<? extends SomeData>[] value() default {};
}
