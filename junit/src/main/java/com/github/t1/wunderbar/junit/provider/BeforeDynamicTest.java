package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// JUnit-Jupiter calls [BeforeEach][org.junit.jupiter.api.BeforeEach] and [AfterEach][org.junit.jupiter.api.AfterEach]
/// methods only once before/after a [TestFactory][org.junit.jupiter.api.TestFactory], i.e. once per `bar` file.
/// In contrast, WunderBar runs methods annotated as [BeforeDynamicTest] before every test within a `bar` file
/// (and [BeforeInteraction] methods before every interaction within one test; see there for details).
/// You can use this annotation on a method to, e.g., set up _all the data_ you need in your service to comply with the test.
///
/// If there are several methods annotated as [BeforeDynamicTest], their execution order is not defined;
/// but you can use the [Order][org.junit.jupiter.api.Order] annotation to specify the order explicitly.
///
/// The annotated method can optionally take parameters of these types:
/// - `List<`[HttpInteraction]`>`: expected interactions,
/// - `List<`[HttpRequest]`>`: expected requests,
/// - `List<`[HttpResponse]`>`: expected responses,
/// - [WunderBarExecutions][com.github.t1.wunderbar.junit.provider.WunderBarExecutions]: meta data about the running dynamic test.
///
/// These lists are immutable; in order to change the interactions, the annotated method can optionally return an object
/// of one of these types. You can use this to, e.g., replace the dummy credentials with real ones:
/// - `List<`[HttpInteraction]`>`: expected interactions,
/// - `List<`[HttpRequest]`>`: expected requests,
/// - `List<`[HttpResponse]`>`: expected responses.
///
/// Note: it's generally easier to use [BeforeInteraction], esp. if you have interactions that mutate the result of previous interactions.
///
/// You can actually add or remove expected HttpInteractions, e.g., to filter the tests that actually need to run.
///
/// See also: [BeforeInteraction]
@Retention(RUNTIME)
public @interface BeforeDynamicTest {}
