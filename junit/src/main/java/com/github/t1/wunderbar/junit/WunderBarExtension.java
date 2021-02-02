package com.github.t1.wunderbar.junit;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@ExtendWith(WunderBarJUnit.class)
public @interface WunderBarExtension {
}
