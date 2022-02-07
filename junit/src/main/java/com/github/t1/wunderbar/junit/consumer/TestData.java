package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.UUID;

/**
 * Generates random values, but tries to keep them positive and small, so they are easier to handle.
 * Generally, it's better to use the {@link Some @Some} annotation.
 */
public class TestData {
    private static int nextInt = Math.abs(new Random().nextInt(9)); // just a bit of initial randomness

    static Object some(Class<?> type) {
        if (boolean.class.equals(type) || Boolean.class.equals(type)) return someBoolean();
        if (byte.class.equals(type) || Byte.class.equals(type)) return someByte();
        if (char.class.equals(type) || Character.class.equals(type)) return someChar();
        if (short.class.equals(type) || Short.class.equals(type)) return someShort();
        if (int.class.equals(type) || Integer.class.equals(type)) return someInt();
        if (long.class.equals(type) || Long.class.equals(type)) return (long) someInt();
        if (BigInteger.class.equals(type)) return BigInteger.valueOf(someInt());
        if (float.class.equals(type) || Float.class.equals(type)) return someFloat();
        if (double.class.equals(type) || Double.class.equals(type)) return someDouble();
        if (BigDecimal.class.equals(type)) return BigDecimal.valueOf(someInt());
        if (String.class.equals(type)) return someString();
        if (UUID.class.equals(type)) return someUUID();
        throw new WunderBarException("don't know how to generate a random " + type.getSimpleName());
    }

    public static boolean someBoolean() {return someInt() % 2 == 0;}

    public static byte someByte() {return (byte) (someInt() % Byte.MAX_VALUE);}

    public static char someChar() {return Character.toChars(someInt())[0];}

    public static short someShort() {return (short) (someInt() % Short.MAX_VALUE);}

    public static int someInt() {return nextInt++;}

    public static float someFloat() {return Float.parseFloat("1." + someInt());}

    public static double someDouble() {return Double.parseDouble("0." + someInt());}

    public static String someId() {return "id-" + someInt();}

    public static String someString() {return "string-" + someInt();}

    public static UUID someUUID() {return testUuidFromInt(someInt());}


    /** UUID useful for testing with all leading zeroes, except for the last part derived from the int */
    public static UUID testUuidFromInt(int i) {
        return UUID.fromString("00000000-0000-0000-0000-" + (100000000000L + i));
    }
}
