package com.github.t1.wunderbar.junit.runner;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@ExtendWith(WunderBarRunnerJUnitExtension.class)
@Inherited
public @interface WunderBarRunnerExtension {
    String baseUri();
}
