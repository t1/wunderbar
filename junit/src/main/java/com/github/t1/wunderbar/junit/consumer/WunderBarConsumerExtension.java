package com.github.t1.wunderbar.junit.consumer;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static com.github.t1.wunderbar.junit.consumer.Level.AUTO;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@ExtendWith(WunderBarConsumerJUnit.class)
@Inherited
public @interface WunderBarConsumerExtension {
    Level level() default AUTO;
}
