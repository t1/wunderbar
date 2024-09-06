package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.common.Internal;
import org.junit.jupiter.api.Order;

import java.lang.reflect.AnnotatedElement;
import java.util.Comparator;

@Internal
public class JunitUtils {
    public static final Comparator<AnnotatedElement> ORDER = Comparator.comparingInt(element ->
            element.isAnnotationPresent(Order.class) ? element.getAnnotation(Order.class).value() : Order.DEFAULT);
}
