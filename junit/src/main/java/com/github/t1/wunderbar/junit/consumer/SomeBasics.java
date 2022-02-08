package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Generates random values, but tries to keep them positive, unique, and small, so they are as easy to handle as possible.
 * These prerequisites are not achievable for booleans and bytes, as they would overflow too fast.
 * <p>
 * The default offset is 1000, i.e. the first value generated will start between 1000 and 1009.
 * <p>
 * Generally, it's better to use the {@link Some @Some} annotation than the <code>some...</code> methods defined here.
 */
public class SomeBasics implements SomeData {
    private static int nextInt;

    static {reset();}

    public static void reset() {reset(1000);}

    public static void reset(int offset) {
        nextInt = offset + Math.abs(new Random().nextInt(10)); // just a bit of initial randomness
    }

    @Override public boolean canGenerate(Class<?> type) {
        return generator(type) != null;
    }

    public <T> T some(Class<T> type) {
        var generator = generator(type);
        if (generator == null) throw new WunderBarException("don't know how to generate a random " + type.getSimpleName());
        return generator.get();
    }

    @SuppressWarnings("unchecked")
    private <T> Supplier<T> generator(Class<T> type) {
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

    public static char someChar() {return Character.toChars(someInt())[0];}

    public static short someShort() {return (short) someInt();}

    public static int someInt() {return nextInt++;}

    public static long someLong() {return someInt();}

    public static float someFloat() {return Float.parseFloat("1." + someInt());}

    public static double someDouble() {return Double.parseDouble("0." + someInt());}

    public static String someId() {return "id-" + someInt();}

    public static String someString() {return "string-" + someInt();}

    public static UUID someUUID() {return testUuidFromInt(someInt());}

    public static URI someURI() {return testUriFromInt(someInt());}

    public static URL someURL() {return testUrlFromInt(someInt());}


    /** UUID useful for testing with all leading zeroes, except for the last part derived from the int */
    public static UUID testUuidFromInt(int i) {
        return UUID.fromString("00000000-0000-0000-0000-" + (100000000000L + i));
    }

    /** URI useful for testing, derived from the int */
    public static URI testUriFromInt(int i) {return URI.create(String.format("https://example.nowhere/path-%07d", i));}

    /** URL useful for testing, derived from the int */
    @SneakyThrows(MalformedURLException.class)
    public static URL testUrlFromInt(int i) {return testUriFromInt(i).toURL();}
}
