package com.github.t1.wunderbar.junit.runner;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures and prepares the tests of the implementation of an API that are found by one of the {@link org.junit.jupiter.api.TestFactory test factories}
 * using {@link WunderBarTestFinder#findTestsIn}, by running the methods annotated {@link BeforeDynamicTest} and {@link AfterDynamicTest}.
 * <p>
 * You may have to start and stop the service you're testing, but that's beyond the scope of WunderBar.
 * <p>
 * Instead of using fixed ids et.al. for the various behavior, it's much better to derive the expected data (or errors) from the expectations
 * defined in the <code>bar</code>. You can add and remove that test data by accessing directly, e.g. the database of your service, or by
 * using other methods of your API to manipulate it through your service. Sometimes you may need to add a 'backdoor' that is only accessible
 * from your own test setup/cleanup code.
 */
@Retention(RUNTIME)
@ExtendWith(WunderBarRunnerJUnitExtension.class)
@Inherited
public @interface WunderBarRunnerExtension {
    /**
     * Where the service runs; without the path that's part of the api calls (and includes the technology).
     * E.g. for a GraphQL service responding to requests on <code>http://localhost:8080/myapp/graphql</code>,
     * or a REST service responding to requests like <code>http://localhost:8080/myapp/rest/ping</code>,
     * the baseUri is <code>http://localhost:8080/myapp</code>, while the BAR contains requests with a path <code>/graphql</code>
     * or <code>/rest/ping</code>.
     */
    String baseUri();
}
