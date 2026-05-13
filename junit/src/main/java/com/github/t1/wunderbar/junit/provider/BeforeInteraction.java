package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.http.HttpInteraction;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// You can use this annotation on a method to, e.g., set up the data you need in your service to comply with this interaction.
///
/// If there are several methods annotated as [BeforeInteraction], their execution order is not defined;
/// but you can use the [Order][org.junit.jupiter.api.Order] annotation to specify the order explicitly.
///
/// The annotated method can optionally take parameters of these types:
/// - [HttpInteraction]: expected interactions,
/// - [HttpRequest]: expected requests,
/// - [HttpResponse]: expected responses,
/// - [WunderBarExecution][com.github.t1.wunderbar.junit.provider.WunderBarExecution]: meta data about the running interaction.
///
/// The annotated method can optionally return an object of one of these types to change the expectations,
/// e.g. to replace the dummy credentials with real ones:
/// - [HttpInteraction]: expected interactions,
/// - [HttpRequest]: expected requests,
/// - [HttpResponse]: expected responses.
///
/// See also: [BeforeDynamicTest], [AfterInteraction]
@Retention(RUNTIME)
public @interface BeforeInteraction {}
