package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;

import java.lang.reflect.Method;
import java.net.URI;

public @Internal interface WunderBarExpectations {
    URI baseUri();

    /** the proxy can be used in two roles. this is the first: for the stubbing in the test class */
    default Object asStubbingProxy(Object proxy) {return proxy;}

    /** the proxy can be used in two roles. this is the second: for the actual calls in the system under test */
    default Object asSutProxy(Object proxy) {return proxy;}

    Object invoke(Method method, Object... args);

    /** the test is finished and the proxy won't be used anymore */
    void done();
}
