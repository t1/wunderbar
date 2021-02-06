package com.github.t1.wunderbar.junit;

import static java.util.Locale.ROOT;

public enum Level {
    /**
     * Use Mockito to stub the invocation.
     * This is generally done in a test class ending with <code>Test</code>.
     */
    UNIT,

    /**
     * Start an http service locally and save all requests and responses in a <code>bar</code> file.
     * This is generally done in a test class ending with <code>IT</code>.
     * This is the default.
     */
    INTEGRATION;

    @Override public String toString() { return name().toLowerCase(ROOT); }
}
