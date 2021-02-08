package com.github.t1.wunderbar.junit;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static com.github.t1.wunderbar.junit.Level.AUTO;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@ExtendWith(WunderBarCustomerJUnit.class)
@Inherited
public @interface WunderBarCustomerExtension {
    Level level() default AUTO;
}
