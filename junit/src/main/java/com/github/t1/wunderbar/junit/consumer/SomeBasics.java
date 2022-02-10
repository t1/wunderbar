package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;
import lombok.SneakyThrows;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Generates random values, but tries to keep them small, positive, and unique, so they are as easy to handle as possible.
 * The default starting point is 1000, so the generated values won't interfere with constants in your code.
 * These prerequisites are not achievable for booleans and bytes, as they would overflow too fast.
 * <p>
 * You can change the starting point by calling {@link #reset(int)}, e.g. in a {@link org.junit.jupiter.api.BeforeEach} method.
 */
public class SomeBasics implements SomeData {
    public static final int DEFAULT_START = 1000;

    private static int nextInt;

    static {reset();}

    public static void reset() {reset(DEFAULT_START);}

    public static void reset(int start) {nextInt = start;}

    @Override public boolean canGenerate(Type type) {
        return generator(type) != null;
    }

    public <T> T some(Type type) {
        Supplier<T> generator = generator(type);
        if (generator == null) throw new WunderBarException("don't know how to generate a random " + type);
        return generator.get();
    }

    @SuppressWarnings("unchecked")
    private <T> Supplier<T> generator(Type type) {
        if (char.class.equals(type) || Character.class.equals(type)) return () -> (T) (Character) someChar();
        if (short.class.equals(type) || Short.class.equals(type)) return () -> (T) (Short) someShort();
        if (int.class.equals(type) || Integer.class.equals(type)) return () -> (T) (Integer) someInt();
        if (long.class.equals(type) || Long.class.equals(type)) return () -> (T) (Long) someLong();
        if (BigInteger.class.equals(type)) return () -> (T) BigInteger.valueOf(someInt());

        if (float.class.equals(type) || Float.class.equals(type)) return () -> (T) (Float) someFloat();
        if (double.class.equals(type) || Double.class.equals(type)) return () -> (T) (Double) someDouble();
        if (BigDecimal.class.equals(type)) return () -> (T) BigDecimal.valueOf(someInt());

        if (String.class.equals(type)) return () -> (T) someString();
        if (UUID.class.equals(type)) return () -> (T) someUUID();
        if (URI.class.equals(type)) return () -> (T) someURI();
        if (URL.class.equals(type)) return () -> (T) someURL();

        return null;
    }

    private static char someChar() {return Character.toChars(someInt())[0];}

    private static short someShort() {return (short) someInt();}

    private static int someInt() {
        if (nextInt >= Short.MAX_VALUE)
            throw new IllegalStateException("too many values generated (we want to prevent a overflow causing non-unique value)");
        return nextInt++;
    }

    private static long someLong() {return someInt();}

    private static float someFloat() {return Float.parseFloat(someInt() + ".1");}

    private static double someDouble() {return Double.parseDouble(someInt() + ".2");}

    private static String someString() {return String.format("string-%05d", someInt());}

    private static UUID someUUID() {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", someInt()));
    }

    private static URI someURI() {return URI.create(String.format("https://example.nowhere/path-%07d", someInt()));}

    @SneakyThrows(MalformedURLException.class)
    private static URL someURL() {return someURI().toURL();}
}
