package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A generator for {@link Some} test data (see there).
 * <p>
 * If you only want to generate a single type in a generator, it's easier to extend {@link SomeSingleTypeData}.
 */
public interface SomeData {
    /**
     * Is this generator capable to generate values of this type? The type could be a generic type, e.g.
     * if your generator is checked whether it can generate a value for a field <code>@Some List&lt;String&gt; x</code>,
     * the <code>Type</code> will be a {@link ParameterizedType}
     * with a {@link ParameterizedType#getRawType() raw type} <code>List</code>
     * and a {@link ParameterizedType#getActualTypeArguments() type argument list} of <code>[String]</code>
     */
    boolean canGenerate(Some some, Type type, AnnotatedElement location);

    /**
     * Generate that value. The type could be a generic type, e.g.
     * if your generator is checked whether it can generate a value for a field <code>@Some List&lt;String&gt; x</code>,
     * the <code>Type</code> will be a {@link ParameterizedType}
     * with a {@link ParameterizedType#getRawType() raw type} <code>List</code>
     * and a {@link ParameterizedType#getActualTypeArguments() type argument list} of <code>[String]</code>
     */
    <T> T some(Some some, Type type, AnnotatedElement location);
}
