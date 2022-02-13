package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Convenience {@link SomeData} class to generate values of a single type automatically derived from
 * the generic type parameter <code>T</code>.
 * You just need to override the {@link #some(Some, Type, AnnotatedElement)} method.
 * <p>
 * A common use-case is that you want your generator to work only for some tags in <code>@Some</code>.
 * To do so, you can simply annotate the type parameter like this:
 * <pre>
 * class CustomId extends SomeSingleTypeData<&#064;Some("id") String> {
 *     public String some(Some some, Type type, AnnotatedElement location) {
 *         return "custom-id";
 *     }
 * }
 * </pre>
 * <p>
 * This also works for generic types, e.g. a type <code>CustomGeneric&lt;?&gt;</code> can be generated by a
 * <code>class CustomGenericGenerator extends SomeSingleTypeData&lt;CustomGeneric&lt;?&gt;&gt;</code>.
 * You can then safely cast the <code>Type</code> parameter in your <code>some</code> method,
 * e.g. <code>var nestedType = ((ParameterizedType) type).getActualTypeArguments()[0];</code>
 */
@SuppressWarnings("unchecked")
public abstract class SomeSingleTypeData<T> implements SomeData {
    protected final Class<T> type;
    protected final Some some;

    public SomeSingleTypeData() {
        var typeOfT = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.type = (Class<T>) ((typeOfT instanceof ParameterizedType) ? rawType(typeOfT) : typeOfT);
        this.some = ((AnnotatedParameterizedType) this.getClass().getAnnotatedSuperclass())
            .getAnnotatedActualTypeArguments()[0].getAnnotation(Some.class);
    }

    @Override public boolean canGenerate(Some some, Type type, AnnotatedElement location) {
        if (type instanceof ParameterizedType) type = rawType(type);
        return this.type.equals(type) && matches(some);
    }

    private boolean matches(Some some) {
        if (this.some == null) return true;
        if (some == null) return this.some.value().length == 0;
        var actual = Arrays.asList(some.value());
        return Stream.of(this.some.value()).allMatch(actual::contains);
    }

    private static Type rawType(Type type) {return ((ParameterizedType) type).getRawType();}

    @Override public abstract T some(Some some, Type type, AnnotatedElement location);
}
