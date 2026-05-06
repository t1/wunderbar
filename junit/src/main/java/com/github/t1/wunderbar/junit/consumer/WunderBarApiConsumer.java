package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.ContractFormat;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static com.github.t1.wunderbar.junit.consumer.Level.AUTO;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures and prepares the tests for some code that consumes an API, by injecting the {@link Service} and {@link SystemUnderTest}
 * fields. Also manages the contract files written.
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
     * The contract files to save interactions to.
     * <p>
     * This is the only consumer-side file output configuration.
     * The default writes one BAR file to <code>target/wunder.bar</code>.
     * Use an empty array to disable writing completely, e.g. <code>@WunderBarApiConsumer(output = {})</code>.
     * <p>
     * BAR files can also be written to a <code>.jar</code> file, and if the file name ends with a slash
     * (<code>/</code>), the test files will not be zipped but remain plain files.
     * OpenAPI output must be a single JSON file, e.g. <code>target/openapi.json</code>.
     * <p>
     * Use several {@link Output outputs} to write more than one contract file from the same test.
     * <p>
     * Will be ignored for {@link Level#UNIT UNIT} level tests.
     */
    Output[] output() default {@Output(fileName = "target/wunder.bar")};

    /**
     * One configured contract output.
     * <p>
     * Example:
     * <pre><code>
     * &#64;WunderBarApiConsumer(output = {
     *     &#64;Output(fileName = "target/wunderbar.jar"),
     *     &#64;Output(format = OPENAPI, fileName = "target/openapi.json")
     * })
     * </code></pre>
     */
    @interface Output {
        /**
         * The format of the contract file to write.
         * <p>
         * The default {@link ContractFormat#AUTO AUTO} resolves to {@link ContractFormat#OPENAPI OPENAPI}
         * for <code>*.json</code> files and to {@link ContractFormat#BAR BAR} otherwise.
         */
        ContractFormat format() default ContractFormat.AUTO;

        /**
         * The path to the contract file to save interactions to.
         * <p>
         * This must not be blank.
         */
        String fileName();
    }
}
