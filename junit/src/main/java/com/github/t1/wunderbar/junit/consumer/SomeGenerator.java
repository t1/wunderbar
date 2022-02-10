package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.Type;

/**
 * You can inject an instance of this type into the constructor of your {@link SomeData} class,
 * or a test lifecycle method or field, to manually generate data, i.e. if you can't use the {@link Some} annotation.
 */
public interface SomeGenerator {
    <T> T generate(Type type, String location);
}
