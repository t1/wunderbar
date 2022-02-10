package com.github.t1.wunderbar.junit.consumer;

import lombok.SneakyThrows;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * You can inject an instance of this type into the constructor of your {@link SomeData} class,
 * or a test lifecycle method or field, to manually generate data, i.e. if you can't use the {@link Some} annotation.
 */
public interface SomeGenerator {
    /**
     * Generate an instance of type <code>T</code>
     *
     * @param some     The {@link Some} annotation at the <code>location</code>
     * @param type     The type to be generated; sometimes a {@link java.lang.reflect.ParameterizedType}
     * @param location The annotated field or parameter; sometimes <code>null</code>
     */
    Object generate(Some some, Type type, AnnotatedElement location);

    /**
     * Convenience overload with a default {@link Some}, a <code>null</code> location,
     * and a generic return type derived from the <code>Class&lt;T&gt;</code> type argument.
     * This is what most callers will need.
     */
    @SuppressWarnings("unchecked")
    default <T> T generate(Class<T> type) {return (T) generate(Some.LITERAL, type, null);}

    /** Convenience overload for a field in a container class. */
    @SuppressWarnings("unchecked")
    @SneakyThrows(ReflectiveOperationException.class)
    default <T> T generate(Class<?> container, String fieldName) {
        Field field = container.getDeclaredField(fieldName);
        return (T) generate(field.getAnnotation(Some.class), field.getGenericType(), field);
    }
}
