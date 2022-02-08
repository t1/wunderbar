package com.github.t1.wunderbar.junit.consumer;

/** A generator for {@link Some} test data (see there). */
public interface SomeData {
    boolean canGenerate(Class<?> type);
    <T> T some(Class<T> type);
}
