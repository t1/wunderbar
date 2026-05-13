package com.github.t1.wunderbar.junit.consumer;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// The [WunderBarApiConsumer] extension will do basic dependency injection for fields annotated as SUT, namely
/// inject those beans it has created for the fields annotated as [Service].
/// If you need more sophisticated dependency injection, i.e. other types, deeply nested classes, interceptors, etc.,
/// you can use a full dependency injection (test) framework, e.g., `weld-junit5`.
/// For more details see the [README](https://github.com/t1/wunderbar#full-dependency-injection).
@Retention(RUNTIME)
public @interface SystemUnderTest {}
