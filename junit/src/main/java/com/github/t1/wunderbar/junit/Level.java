package com.github.t1.wunderbar.junit;

public enum Level {
    /**
     * Use Mockito to directly stub the invocation.
     * This is generally done in a test class ending with <code>Test</code>.
     */
    UNIT,

    /**
     * Start an http service locally and save all requests and responses in a <code>bar</code> file.
     * This is generally done in a test class ending with <code>IT</code>.
     * This is the default.
     */
    INTEGRATION
}
