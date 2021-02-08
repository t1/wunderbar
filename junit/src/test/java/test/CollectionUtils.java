package test;

import java.lang.reflect.Array;
import java.util.List;

public class CollectionUtils {
    public static <T> List<T> and(List<T> list, T element) {
        if (list == null) return List.of(element);
        @SuppressWarnings("unchecked")
        var array = (T[]) Array.newInstance(element.getClass(), list.size() + 1);
        array[array.length - 1] = element;
        return List.of(array);
    }
}
