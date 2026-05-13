package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.consumer.SomeData;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Register these classes as [test data generators][com.github.t1.wunderbar.junit.consumer.SomeData]
/// to be usable with [@Some][com.github.t1.wunderbar.junit.consumer.Some].
///
/// Note that you can put this annotation on [nested][org.junit.jupiter.api.Nested] test classes as well as test methods,
/// and they will only be used in that scope.
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Register {
    Class<? extends SomeData>[] value() default {};
}
