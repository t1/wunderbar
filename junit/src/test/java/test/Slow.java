package test;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Tag("SLOW")
@Retention(RUNTIME)
public @interface Slow {}
