package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A generator for {@link Some} test data (see there).
 * <p>
 * If you only want to generate a single type in your generator, it's easier to extend {@link SomeSingleTypes}.
 */
public interface SomeData {
    /**
     * Is this generator capable to generate values of this type?
     *
     * @param some     The <code>@Some</code> annotation on the field/parameter; may be null
     * @param type     The type of the field/parameter; could be a generic type, e.g.
     *                 if the field is <code>@Some List&lt;String&gt; x</code>,
     *                 the <code>Type</code> will be a {@link ParameterizedType}
     *                 with a {@link ParameterizedType#getRawType() raw type} <code>List</code>
     *                 and a {@link ParameterizedType#getActualTypeArguments() type argument list} of <code>[String]</code>
     * @param location The field/parameter to generate the value for
     */
    boolean canGenerate(Some some, Type type, AnnotatedElement location);

    /**
     * Generate that value.
     *
     * @param some     The <code>@Some</code> annotation on the field/parameter; may be null
     * @param type     The type of the field/parameter; could be a generic type, e.g.
     *                 if your generator is checked whether it can generate a value for a field <code>@Some List&lt;String&gt; x</code>,
     *                 the <code>Type</code> will be a {@link ParameterizedType}
     *                 with a {@link ParameterizedType#getRawType() raw type} <code>List</code>
     *                 and a {@link ParameterizedType#getActualTypeArguments() type argument list} of <code>[String]</code>
     * @param location The field/parameter to generate the value for
     * @param <T>      Just a convenience to be easily assignable; may result in ClassCastExceptions at runtime
     */
    <T> T some(Some some, Type type, AnnotatedElement location);
}
