package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// JUnit-Jupiter calls [BeforeEach][org.junit.jupiter.api.BeforeEach] and [AfterEach][org.junit.jupiter.api.AfterEach]
/// methods only once before/after a [TestFactory][org.junit.jupiter.api.TestFactory], i.e. once per `bar` file.
/// In contrast, WunderBar runs methods annotated as [AfterDynamicTest] after every test within a `bar` file
/// (and [AfterInteraction] methods after every interaction within one test; see there for details).
/// You can use this annotation on a method to, e.g., clean up the data you need in your service to comply with the test.
///
/// If there are several methods annotated as [AfterDynamicTest], their execution order is not defined;
/// but you can use the [Order][org.junit.jupiter.api.Order] annotation to specify the order explicitly.
///
/// The annotated method can optionally take parameters of these types:
/// - `List<`[HttpInteraction]`>`: expected interactions,
/// - `List<`[HttpRequest]`>`: expected requests,
/// - `List<`[HttpResponse]`>`: expected responses,
/// - [WunderBarExecutions][com.github.t1.wunderbar.junit.provider.WunderBarExecutions]: meta data about the running dynamic test.
///
/// Note: it's sometimes easier to use [AfterInteraction], esp. if you have only a single interaction in a test,
/// you don't need the overhead with the lists.
///
/// See also: [AfterInteraction]
@Retention(RUNTIME)
public @interface AfterDynamicTest {}
