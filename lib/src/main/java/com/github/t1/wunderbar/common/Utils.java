package com.github.t1.wunderbar.common;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonPatch.Operation.ADD;

@UtilityClass
public @Internal class Utils {

    @SneakyThrows(ReflectiveOperationException.class)
    public static Object invoke(Object instance, Method method, Object... args) {
        method.setAccessible(true);
        try {
            return method.invoke(instance, args);
        } catch (IllegalArgumentException e) {
            if ("argument type mismatch".equals(e.getMessage()))
                throw new IllegalArgumentException(method + " doesn't like these argument types:" + argumentTypes(args), e);
            throw e;
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException)
                throw (RuntimeException) e.getTargetException();
            if (e.getTargetException() instanceof AssertionError)
                throw (AssertionError) e.getTargetException();
            throw e;
        }
    }

    private static List<String> argumentTypes(Object[] args) {
        return Stream.of(args).map(Object::getClass).map(Class::getSimpleName).collect(toList());
    }

    @SneakyThrows(IOException.class)
    public static void deleteRecursive(Path path) {
        if (Files.exists(path)) {
            var list = Files.walk(path).collect(toList());
            Collections.reverse(list);
            list.forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException("can't delete " + p, e);
                }
            });
        }
    }

    @SneakyThrows(NoSuchFieldException.class)
    public static <T> T getField(Object instance, String fieldName) {
        Field field = instance.getClass().getDeclaredField(fieldName);
        return getField(instance, field);
    }

    @SneakyThrows(IllegalAccessException.class)
    public static <T> T getField(Object instance, Field field) {
        field.setAccessible(true);
        //noinspection unchecked
        return (T) field.get(instance);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    public static void setField(Object instance, Field field, Object value) {
        field.setAccessible(true);
        field.set(instance, value);
    }

    public static Stream<JsonValue> jsonNonAddDiff(JsonValue expected, JsonValue actual) {
        if (expected.getValueType() != actual.getValueType()) {
            expected = Json.createObjectBuilder().add("diff-dummy", expected).build();
            actual = Json.createObjectBuilder().add("diff-dummy", actual).build();
        }
        return Json.createDiff((JsonStructure) expected, (JsonStructure) actual).toJsonArray().stream()
            .filter(Utils::isNonAdd);
    }

    private static boolean isNonAdd(JsonValue jsonValue) {
        return !jsonValue.asJsonObject().getString("op").equals(ADD.operationName());
    }

    public static String prefix(String prefix, String string) {
        return (string == null) ? prefix + "null" :
            Arrays.stream(string.split("\n"))
                .map(line -> prefix + line)
                .collect(joining("\n"));
    }

    /** {@link AnnotatedElement} doesn't declare a <code>getName</code> method, although most implementations do. */
    public static String name(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Class) return ((Class<?>) annotatedElement).getName();
        if (annotatedElement instanceof Member) return ((Member) annotatedElement).getName();
        if (annotatedElement instanceof Parameter) return ((Parameter) annotatedElement).getName();
        return null;
    }
}
