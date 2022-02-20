package com.github.t1.wunderbar.junit.consumer;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;

import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Generate unique data to be used for a {@link org.junit.jupiter.api.extension.ParameterResolver parameter or field}.
 * Whenever a value is generated, it's logged, including the location where it is used for; this makes handling easier, e.g. debugging.
 * The basic set of value types are generated by the {@link SomeBasics}, and you can {@link com.github.t1.wunderbar.junit.Register register}
 * your own {@link SomeData generators}.
 * <p>
 * Depending on the actually generated values makes your test brittle: it's better to use the field/parameter names instead.
 * But if you are forced to, you may need the {@link org.junit.jupiter.api.Order junit Order} annotation to define
 * the generation order of fields.
 */
@Target(TYPE_USE)
@Retention(RUNTIME)
public @interface Some {
    /** A list of "tags" passed to {@link SomeData custom generators} for fine-control, e.g. <code>invalid</code>. */
    String[] value() default {};

    SomeLiteral LITERAL = new SomeLiteral(new String[0]);

    @Value @With @Accessors(fluent = true) @EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
    class SomeLiteral extends AnnotationLiteral<Some> implements Some {
        String[] value;

        public SomeLiteral withTags(String... values) {return withValue(values);}

        public String toString(Some some) {return (some == null) ? "" : "@Some(" + String.join(", ", some.value()) + ")";}
    }
}
