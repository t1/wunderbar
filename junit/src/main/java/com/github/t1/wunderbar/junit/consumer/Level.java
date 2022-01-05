package com.github.t1.wunderbar.junit.consumer;

import static java.util.Locale.ROOT;

/**
 * The mode to run a test annotated as {@link WunderBarApiConsumer}.
 * The default is {@link #AUTO}, so the level is determined by the test name.
 */
public enum Level {
    /**
     * Automatically determine the level from the test name:<ul>
     * <li> If the name ends with <code>ST</code>, it's an {@link #SYSTEM} test
     * <li> If the name ends with <code>IT</code>, it's an {@link #INTEGRATION} test
     * <li> otherwise it's a {@link #UNIT} test
     * </ul>
     * This is the default.
     */
    AUTO,

    /**
     * Use Mockito to stub the expectation. The fastest turn-around.
     * <p>
     * This is generally done in a test class ending with <code>Test</code>.
     */
    UNIT,

    /**
     * Start an http service locally and save all requests and responses in a <code>bar</code> file.
     * This tests the actual annotations on your API interface as well as the de/serialization.
     * And it specifies the API you expect a service to provide, even before that service exists.
     * <p>
     * This is generally done in a test class ending with <code>IT</code>.
     */
    INTEGRATION,

    /**
     * There are two options:
     * <p>
     * 1) Call a real service, so you can explore the behavior of an existing API.
     * Building expectations with {@link WunderbarExpectationBuilder#given given} doesn't make any sense in this case
     * and will result in premature calls to the service.
     * This may be irritating, but we can't distinguish these calls from valid ones.
     * <p>
     * 2) Call an already installed <code>wunderbar-mock-server</code>.
     * How you do this and how you configure your service to call the mock-server is out of the scope of WunderBar.
     * <p>
     * Use the {@link WunderBarApiConsumer#endpoint()} to configure where the service or mock-service run.
     * <p>
     * This level is chosen automatically if a test class ends with <code>ST</code>.
     */
    SYSTEM;

    @Override public String toString() {return name().toLowerCase(ROOT);}
}
