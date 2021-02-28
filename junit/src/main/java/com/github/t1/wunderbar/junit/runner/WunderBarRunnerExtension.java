package com.github.t1.wunderbar.junit.runner;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Use {@link WunderBarRunner} instead */
@Deprecated(since = "1.2", forRemoval = true)
@Retention(RUNTIME)
@ExtendWith(WunderBarRunnerJUnitExtension.class)
@Inherited
public @interface WunderBarRunnerExtension {
    String baseUri();
}
