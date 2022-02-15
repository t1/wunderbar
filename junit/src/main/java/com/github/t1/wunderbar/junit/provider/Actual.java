package com.github.t1.wunderbar.junit.provider;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotate a {@link com.github.t1.wunderbar.junit.http.HttpResponse HttpResponse} parameter of a
 * {@link AfterInteraction} method, so the response will not be the response from the BAR file,
 * but the actual at that your service has returned.
 * This is necessary, e.g. when your database creates a primary key, so you can take the actual at
 * to manipulate the expected response.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Actual {}
