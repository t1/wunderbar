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

    /**
     * The path to the <code>bar</code> file to save interactions to; or {@link #NONE}, if they should <em>not</em> be saved.
     * <p>
     * Not applicable to {@link Level#UNIT UNIT} tests.
     */
    String fileName() default "target/wunder.bar";

    /**
     * Base uri template where a service needed for a {@link Level#SYSTEM SYSTEM} level test runs.
     * The template variable <code>technology</code> will be replaced by <code>graphql</code> or <code>rest</code> respectively.
     */
    String endpoint() default "http://localhost:8080/{technology}";

    /** Indicates that <em>no</em> <code>bar</code> file should be written. */
    String NONE = "";
}
