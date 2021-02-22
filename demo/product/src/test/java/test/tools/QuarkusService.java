package test.tools;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Just for internal use. It would be nice to use just `@QuarkusTest`, but that fails, as the `AfterEach` of the
 * `WunderBarRunnerExtension` is called before the dynamic tests are run.
 */
@Retention(RUNTIME)
@ExtendWith(QuarkusServiceExtension.class)
public @interface QuarkusService {}
