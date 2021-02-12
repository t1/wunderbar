package com.github.t1.wunderbar.junit.consumer;

import static java.util.Locale.ROOT;

public enum Level {
    /**
     * Automatically determine the level from the test name:
     * - If the name ends with <code>IT</code>, it's an INTEGRATION test
     * - otherwise it's a UNIT test
     */
    AUTO,

    /**
     * Use Mockito to stub the expectation.
     * This is generally done in a test class ending with <code>Test</code>.
     */
    UNIT,

    /**
     * Start an http service locally and save all requests and responses in a <code>bar</code> file.
     * This is generally done in a test class ending with <code>IT</code>.
     */
    INTEGRATION;

    @Override public String toString() { return name().toLowerCase(ROOT); }
}
