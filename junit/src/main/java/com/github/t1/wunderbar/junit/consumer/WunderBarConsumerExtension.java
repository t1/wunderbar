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
     * Note that you can also write to a <code>.jar</code> file, the runner accepts that and your tooling may be better.
     * And if the file name ends with a slash (<code>/</code>), the test files will not be zipped but remain plain files;
     * this may also be more convenient for some use cases.
     * <p>
     * Not applicable to {@link Level#UNIT UNIT} tests.
     */
    String fileName() default "target/wunder.bar";

    /**
     * Base uri template where a service needed for a {@link Level#SYSTEM SYSTEM} level test runs.
     * <ol>
     * <li>A method template variable like <code>{foo()}</code> will be replaced by the result of a call to the (maybe static) method
     *     of that name in the test class.
     * <li>The template variable <code>technology</code> will be replaced by <code>graphql</code> or <code>rest</code> respectively.
     * </ol>
     */
    String endpoint() default "http://localhost:8080/{technology}";

    /** Indicates that <em>no</em> <code>bar</code> file should be written. */
    String NONE = "";
}
