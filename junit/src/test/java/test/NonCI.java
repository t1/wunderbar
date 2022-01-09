package test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Tag("non-ci")
@DisabledIfSystemProperty(named = "CI", matches = ".*")
@Retention(RUNTIME)
public @interface NonCI {}
