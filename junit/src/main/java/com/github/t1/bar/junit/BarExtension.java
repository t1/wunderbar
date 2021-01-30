package com.github.t1.bar.junit;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@ExtendWith(BarJUnit.class)
public @interface BarExtension {
}
