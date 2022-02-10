package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Predicate;

/** A generator for {@link Some} test data (see there). */
public interface SomeData {
    /**
     * Is this generator capable to generate values of this type? The type could be a generic type, e.g.
     * if your generator is checked whether it can generate a value for a field <code>@Some List&lt;String&gt; x</code>,
     * the <code>Type</code> will be a {@link ParameterizedType}
     * with a {@link ParameterizedType#getRawType() raw type} <code>List</code>
     * and a {@link ParameterizedType#getActualTypeArguments() type argument list} of <code>[String]</code>
     */
    boolean canGenerate(Type type);

    /**
     * Generate that value. The type could be a generic type, e.g.
     * if your generator is checked whether it can generate a value for a field <code>@Some List&lt;String&gt; x</code>,
     * the <code>Type</code> will be a {@link ParameterizedType}
     * with a {@link ParameterizedType#getRawType() raw type} <code>List</code>
     * and a {@link ParameterizedType#getActualTypeArguments() type argument list} of <code>[String]</code>
     */
    <T> T some(Type type);

    /** a helper method for cases where the type of {@link #some(Type)} is a parameterized (generics) type */
    static boolean ifParameterized(Type type, Predicate<ParameterizedType> predicate) {
        if (type instanceof ParameterizedType) return predicate.test((ParameterizedType) type);
        else return false;
    }
}
