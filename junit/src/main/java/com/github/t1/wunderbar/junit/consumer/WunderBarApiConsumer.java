package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.ContractFormat;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static com.github.t1.wunderbar.junit.consumer.Level.AUTO;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Configures and prepares the tests for some code that consumes an API, by injecting the [Service] and [SystemUnderTest]
/// fields. Also manages the contract files written.
///
/// When you have [Nested][org.junit.jupiter.api.Nested] tests, the annotation closest to the test determines the configuration.
///
/// It also provides additional [parameters][org.junit.jupiter.api.extension.ParameterResolver] for your tests:
/// - Parameters annotated as [Some] (see there for details).
/// - [SomeGenerator] to generate dynamic test data.
/// - The actual [Level]: mainly useful for testing WunderBar itself.
///
/// See also: [WunderbarExpectationBuilder.given]
@Retention(RUNTIME)
@ExtendWith(WunderBarApiConsumerJUnitExtension.class)
@Inherited
public @interface WunderBarApiConsumer {
    /// The mode to run a test annotated as [WunderBarApiConsumer].
    /// The default is [AUTO][Level.AUTO], so the level is determined by the test name.
    Level level() default AUTO;

    /// The contract files to save interactions to.
    ///
    /// This is the only consumer-side file output configuration.
    /// The default writes one BAR file to `target/wunder.bar`.
    /// Use an empty array to disable writing completely, e.g. `@WunderBarApiConsumer(output = {})`.
    ///
    /// BAR files can also be written to a `.jar` file, and if the file name ends with a slash
    /// (`/`), the test files will not be zipped but remain plain files.
    /// OpenAPI output must be a single JSON file, e.g. `target/openapi.json`.
    ///
    /// OpenAPI output is lossy and suitable only for simple single-interaction cases. Multi-interaction tests may be
    /// skipped during export. Prefer [BAR][ContractFormat.BAR] when you need full-fidelity provider replay.
    ///
    /// Use several [Output] instances to write more than one contract file from the same test.
    ///
    /// Will be ignored for [UNIT][Level.UNIT] level tests.
    Output[] output() default {@Output(fileName = "target/wunder.bar")};

    /// One configured contract output.
    ///
    /// Example:
    /// ```
    /// @WunderBarApiConsumer(output = {
    ///     @Output(fileName = "target/wunderbar.jar"),
    ///     @Output(format = OPENAPI, fileName = "target/openapi.json")
    /// })
    /// ```
    @interface Output {
        /// The format of the contract file to write.
        ///
        /// The default [AUTO][ContractFormat.AUTO] resolves to [OPENAPI][ContractFormat.OPENAPI]
        /// for `*.json` files and to [BAR][ContractFormat.BAR] otherwise.
        ContractFormat format() default ContractFormat.AUTO;

        /// The path to the contract file to save interactions to.
        ///
        /// This must not be blank.
        String fileName();
    }
}
