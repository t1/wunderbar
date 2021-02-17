package com.github.t1.wunderbar.junit.quarkus;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Just for internal use */
@Retention(RUNTIME)
@ExtendWith(QuarkusServiceExtension.class)
public @interface QuarkusService {}
