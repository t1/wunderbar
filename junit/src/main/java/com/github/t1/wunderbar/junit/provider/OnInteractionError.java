package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// You can use this annotation to, e.g., check the result of an unexpectedly failing interaction,
/// i.e. an interaction that doesn't behave as specified in the bar. This is mainly useful for
/// testing WunderBar itself; to clean things up, use [AfterInteraction], which is called
/// directly before [OnInteractionError] methods.
///
/// If there are several methods annotated as [OnInteractionError], their execution order is not defined;
/// but you can use the [Order][org.junit.jupiter.api.Order] annotation to specify the order explicitly.
///
/// The annotated method can take any of these parameters:
/// - [HttpInteraction]: the expected request and response,
/// - [HttpRequest]: the expected request,
/// - [HttpResponse]: the expected response,
/// - [BDDSoftAssertions][org.assertj.core.api.BDDSoftAssertions]: the mismatches that WunderBar has found;
///   call [assertAll()][org.assertj.core.api.BDDSoftAssertions.assertAll] to turn them into an `AssertionError`,
/// - [WunderBarExecution][com.github.t1.wunderbar.junit.provider.WunderBarExecution]: meta data about the running dynamic test.
///
/// See also: [AfterInteraction]
@Retention(RUNTIME)
public @interface OnInteractionError {}
