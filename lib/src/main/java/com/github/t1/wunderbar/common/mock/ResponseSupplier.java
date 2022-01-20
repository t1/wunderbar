package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;

import java.util.function.Function;

public interface ResponseSupplier extends Function<HttpRequest, HttpResponse> {}
