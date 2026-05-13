package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.http.HttpResponse;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Annotate a [HttpResponse] parameter of a [AfterInteraction] method, so the response will not be the response
/// from the BAR file, but the actual one that your service has returned.
/// This is necessary, e.g. when your database creates a primary key, so you can take the actual response
/// to manipulate the expected response.
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Actual {}
