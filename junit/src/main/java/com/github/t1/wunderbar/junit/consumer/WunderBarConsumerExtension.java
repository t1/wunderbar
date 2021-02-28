package com.github.t1.wunderbar.junit.consumer;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static com.github.t1.wunderbar.junit.consumer.Level.AUTO;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** use {@link WunderBarConsumer} instead */
@Deprecated(since = "1.2", forRemoval = true)
@Retention(RUNTIME)
@ExtendWith(WunderBarConsumerJUnitExtension.class)
@Inherited
public @interface WunderBarConsumerExtension {
    Level level() default AUTO;
    String fileName() default "target/wunder.bar";
    String endpoint() default "http://localhost:8080/{technology}";
    String NONE = "";
}
