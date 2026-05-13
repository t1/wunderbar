package com.github.t1.wunderbar.junit.consumer;

import lombok.SneakyThrows;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/// You can inject an instance of this type into the constructor of your [SomeData] class, or a test lifecycle
/// method or field, to manually generate data, i.e. if you can't use the [Some] annotation directly.
public interface SomeGenerator {
    /// Generate an instance of type `T`.
    ///
    /// - `some`: the [Some] annotation at the `location`
    /// - `type`: the type to be generated; sometimes a [ParameterizedType][java.lang.reflect.ParameterizedType]
    /// - `location`: the annotated field or parameter; sometimes `null`
    <T> T generate(Some some, Type type, AnnotatedElement location);

    /// Convenience overload with a default [Some], a `null` location,
    /// and a generic return type derived from the `Class<T>` type argument.
    /// This is what most callers will need.
    default <T> T generate(Class<T> type) {return generate(Some.LITERAL, type, null);}

    /// Convenience overload with a `Class<T>` type.
    default <T> T generate(Some some, Class<T> type, AnnotatedElement location) {return generate(some, (Type) type, location);}

    /// Convenience overload for a named field in a container class.
    @SneakyThrows(ReflectiveOperationException.class)
    default <T> T generate(Class<?> container, String fieldName) {return generate(container.getDeclaredField(fieldName));}

    /// Convenience overload to generate the value for a specific field
    default <T> T generate(Field field) {
        return generate(field.getAnnotatedType().getAnnotation(Some.class), field.getGenericType(), field);
    }

    /// Find the location where this value was generated for.
    ///
    /// Throws [WunderBarException][com.github.t1.wunderbar.junit.WunderBarException] if the generator passed `null` as the location,
    /// or that value was not generated via [Some].
    AnnotatedElement location(Object value);

    /// Find the [Some] used to generate that value.
    ///
    /// Throws [WunderBarException][com.github.t1.wunderbar.junit.WunderBarException] if that value was not generated via [Some].
    Some findSomeFor(Object value);
}
