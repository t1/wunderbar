package com.github.t1.wunderbar.junit.consumer;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;

import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Generate random data to be used for a {@link org.junit.jupiter.api.extension.ParameterResolver parameter or field}.
 * The basic set of value types are generated by the {@link SomeBasics}; and you can {@link com.github.t1.wunderbar.junit.Register register}
 * your own {@link SomeData generators}.
 */
@Retention(RUNTIME)
public @interface Some {
    /** A list of "tags" passed to {@link SomeData custom generators} for fine-control, e.g. <code>invalid</code>. */
    String[] value() default {};

    SomeLiteral LITERAL = new SomeLiteral(new String[0]);

    @Value @With @Accessors(fluent = true) @EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
    class SomeLiteral extends AnnotationLiteral<Some> implements Some {
        String[] value;

        public SomeLiteral withTags(String... values) {return withValue(values);}
    }
}
