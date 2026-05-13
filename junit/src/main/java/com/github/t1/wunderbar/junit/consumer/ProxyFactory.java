package com.github.t1.wunderbar.junit.consumer;

/// This allows the manual generation of the two types of proxies.
/// For [UNIT][Level.UNIT] and [INTEGRATION][Level.INTEGRATION], these are the same,
/// but for [SYSTEM][Level.SYSTEM], this is different.
public interface ProxyFactory<T> {
    /// This is the proxy to use inside the test class for stubbing the expected results.
    T getStubbingProxy();

    /// This is the proxy to use in the system under test, i.e. for the real service calls.
    T getSutProxy();
}
