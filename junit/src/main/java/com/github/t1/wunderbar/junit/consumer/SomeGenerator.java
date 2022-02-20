package com.github.t1.wunderbar.junit.consumer;

import lombok.SneakyThrows;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * You can inject an instance of this type into the constructor of your {@link SomeData} class, or a test lifecycle
 * method or field, to manually generate data, i.e. if you can't use the {@link Some} annotation directly.
 */
public interface SomeGenerator {
    /**
     * Generate an instance of type <code>T</code>
     *
     * @param some     The {@link Some} annotation at the <code>location</code>
     * @param type     The type to be generated; sometimes a {@link java.lang.reflect.ParameterizedType}
     * @param location The annotated field or parameter; sometimes <code>null</code>
     */
    <T> T generate(Some some, Type type, AnnotatedElement location);

    /**
     * Convenience overload with a default {@link Some}, a <code>null</code> location,
     * and a generic return type derived from the <code>Class&lt;T&gt;</code> type argument.
     * This is what most callers will need.
     */
    default <T> T generate(Class<T> type) {return generate(Some.LITERAL, type, null);}

    /**
     * Convenience overload with a <code>Class&lt;T&gt;</code> type.
     */
    default <T> T generate(Some some, Class<T> type, AnnotatedElement location) {return generate(some, (Type) type, location);}

    /** Convenience overload for a field in a container class. */
    @SneakyThrows(ReflectiveOperationException.class)
    default <T> T generate(Class<?> container, String fieldName) {return generate(container.getDeclaredField(fieldName));}

    /** Convenience overload to generate the value for a specific field */
    default <T> T generate(Field field) {
        return generate(field.getAnnotatedType().getAnnotation(Some.class), field.getGenericType(), field);
    }

    /**
     * Find the location where this value was generated for
     *
     * @throws com.github.t1.wunderbar.junit.WunderBarException if the generator passed <code>null</code> as the location,
     *                                                          or that value was not generated via {@link Some}.
     */
    AnnotatedElement location(Object value);

    /**
     * Find the {@link Some} used to generate that value
     *
     * @throws com.github.t1.wunderbar.junit.WunderBarException if that value was not generated via {@link Some}.
     */
    Some findSomeFor(Object value);
}
