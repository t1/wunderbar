package com.github.t1.wunderbar.junit.provider;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Use {@link WunderBarApiProvider} instead */
@Deprecated(since = "1.2", forRemoval = true)
@Retention(RUNTIME)
@ExtendWith(WunderBarApiProviderJUnitExtension.class)
@Inherited
public @interface WunderBarRunnerExtension {
    String baseUri();
}
