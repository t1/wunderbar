package com.github.t1.wunderbar.common;

import lombok.SneakyThrows;

import javax.json.bind.annotation.JsonbTransient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.t1.wunderbar.common.Utils.getField;

public @Internal class DeepEquals {
    public static boolean deeplyEqual(Object left, Object right) {
        if (left == right)
            return true;
        if (left == null || right == null)
            return false;
        if (left instanceof Object[] && right instanceof Object[])
            return deeplyEqualArray((Object[]) left, (Object[]) right);
        if (left instanceof byte[] && right instanceof byte[])
            return Arrays.equals((byte[]) left, (byte[]) right);
        if (left instanceof short[] && right instanceof short[])
            return Arrays.equals((short[]) left, (short[]) right);
        if (left instanceof int[] && right instanceof int[])
            return Arrays.equals((int[]) left, (int[]) right);
        if (left instanceof long[] && right instanceof long[])
            return Arrays.equals((long[]) left, (long[]) right);
        if (left instanceof char[] && right instanceof char[])
            return Arrays.equals((char[]) left, (char[]) right);
        if (left instanceof float[] && right instanceof float[])
            return Arrays.equals((float[]) left, (float[]) right);
        if (left instanceof double[] && right instanceof double[])
            return Arrays.equals((double[]) left, (double[]) right);
        if (left instanceof boolean[] && right instanceof boolean[])
            return Arrays.equals((boolean[]) left, (boolean[]) right);
        if (overridesEquals(left.getClass()))
            return left.equals(right);
        return deeplyEqualObject(left, right);
    }

    private static boolean deeplyEqualArray(Object[] left, Object[] right) {
        if (left.length != right.length)
            return false;

        for (int i = 0; i < left.length; i++)
            if (!deeplyEqual(left[i], right[i]))
                return false;

        return true;
    }

    private static boolean deeplyEqualObject(Object left, Object right) {
        if (!left.getClass().equals(right.getClass()))
            return false;

        for (Class<?> type = left.getClass(); type != null; type = type.getSuperclass())
            for (Field field : left.getClass().getDeclaredFields())
                if (!isTransient(field) && !deeplyEqual(getField(left, field), getField(right, field)))
                    return false;

        return true;
    }

    private static boolean isTransient(Field field) {
        return Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(JsonbTransient.class);
    }

    private static boolean overridesEquals(Class<?> type) {
        return CLASSES_WITH_EQUALS.computeIfAbsent(type, DeepEquals::computeOverridesEquals);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private static Boolean computeOverridesEquals(Class<?> type) {
        return !type.getMethod("equals", Object.class).getDeclaringClass().equals(Object.class);
    }

    private static final Map<Class<?>, Boolean> CLASSES_WITH_EQUALS = new ConcurrentHashMap<>();
}
