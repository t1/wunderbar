package com.github.t1.wunderbar.junit.consumer;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static com.github.t1.wunderbar.junit.consumer.Level.AUTO;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures and prepares the tests for some code that consumes an API, by injecting the {@link Service} and {@link SystemUnderTest}
 * fields. Also manages the <code>bar</code> files written.
 * <p>
 * When you have {@link org.junit.jupiter.api.Nested Nested} tests, the annotation closest to the test determines the configuration.
 * <p>
 * It also provides additional {@link org.junit.jupiter.api.extension.ParameterResolver parameters} for your tests:
 * <ul>
 * <li>Parameters annotated as {@link Some} (see there for details).
 * <li>{@link SomeGenerator} to generate dynamic test data.
 * <li>The actual {@link Level}: mainly useful for testing WunderBar itself.
 * </ul>
 *
 * @see WunderbarExpectationBuilder#given
 */
@Retention(RUNTIME)
@ExtendWith(WunderBarApiConsumerJUnitExtension.class)
@Inherited
public @interface WunderBarApiConsumer {
    /**
     * The mode to run a test annotated as {@link WunderBarApiConsumer}.
     * The default is {@link Level#AUTO AUTO}, so the level is determined by the test name.
     */
    Level level() default AUTO;

    /**
     * The path to the <code>bar</code> file to save interactions to; or {@link #NONE}, if they should <em>not</em> be saved.
     * Defaults to <code>target/wunder.bar</code>.
     * <p>
     * Note that you can also write to a <code>.jar</code> file, WunderBar accepts those, too, and your tooling may be better.
     * And if the file name ends with a slash (<code>/</code>), the test files will not be zipped but remain plain files;
     * this may also be more convenient for some use cases.
     * <p>
     * Will be ignored for {@link Level#UNIT UNIT} level tests.
     */
    String fileName() default "target/wunder.bar";

    /** Indicates that <em>no</em> <code>bar</code> file should be written. */
    String NONE = "";
}
