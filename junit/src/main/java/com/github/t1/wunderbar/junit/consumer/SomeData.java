package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/// A generator for [Some] test data (see there).
///
/// If you only want to generate a single type in your generator, it's easier to extend [SomeSingleTypes].
public interface SomeData {
    /// Generate that value.
    ///
    /// - `some`: the `@Some` annotation on the field/parameter; may be null
    /// - `type`: the type of the field/parameter; could be a generic type, e.g.
    ///   if your generator is checked whether it can generate a value for a field `@Some List<String> x`,
    ///   the `Type` will be a [ParameterizedType]
    ///   with a [raw type][ParameterizedType.getRawType] `List`
    ///   and a [type argument list][ParameterizedType.getActualTypeArguments] of `[String]`
    /// - `location`: the field/parameter to generate the value for
    /// - `T`: just a convenience to be easily assignable; may result in `ClassCastException`s at runtime
    /// - Returns: `Optional.empty()` if the generator can't generate this type of value; `null` can't be generated
    <T> Optional<T> some(Some some, Type type, AnnotatedElement location);
}
