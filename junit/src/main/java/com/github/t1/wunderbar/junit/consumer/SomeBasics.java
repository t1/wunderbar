package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;
import lombok.SneakyThrows;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import static com.github.t1.wunderbar.common.Utils.name;
import static java.time.ZoneOffset.UTC;

/**
 * Generates random values, but tries to keep them small, positive, and unique, so they are as easy to handle as possible.
 * These prerequisites are not achievable for booleans, obviously.
 * Strings, etc., also contain a numeric value to make them unique.
 * The default starting point is 100, so you can use smaller constants in your code without interfering with the generated
 * values. If you need bigger constants, you can change the starting point by calling {@link #reset(int)},
 * e.g. in a {@link org.junit.jupiter.api.BeforeEach} method.
 * <p>
 * Note that the <code>some...</code> methods are public, but the `@Some` annotation comes with superpowers:
 * automatic logging, reset for every test, and most often a location that you can look up.
 */
public class SomeBasics implements SomeData {
    public static final int DEFAULT_START = 100;
    public static final Instant START_INSTANT = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, UTC).toInstant();

    private static int nextInt;

    static {reset();}

    public static void reset() {reset(DEFAULT_START);}

    public static void reset(int start) {nextInt = start;}

    @Override public boolean canGenerate(Some some, Type type, AnnotatedElement location) {
        return generator(some, type, location) != null;
    }

    @Override public <T> T some(Some some, Type type, AnnotatedElement location) {
        Supplier<T> generator = generator(some, type, location);
        if (generator == null) throw new WunderBarException("don't know how to generate a random " + type);
        return generator.get();
    }

    @SuppressWarnings("unchecked")
    private <T> Supplier<T> generator(Some some, Type type, AnnotatedElement location) {
        if (byte.class.equals(type) || Byte.class.equals(type)) return () -> (T) (Byte) someByte();
        if (char.class.equals(type) || Character.class.equals(type)) return () -> (T) (Character) someChar();
        if (short.class.equals(type) || Short.class.equals(type)) return () -> (T) (Short) someShort();
        if (int.class.equals(type) || Integer.class.equals(type)) return () -> (T) (Integer) someInt();
        if (long.class.equals(type) || Long.class.equals(type)) return () -> (T) (Long) someLong();
        if (BigInteger.class.equals(type)) return () -> (T) BigInteger.valueOf(someInt());

        if (float.class.equals(type) || Float.class.equals(type)) return () -> (T) (Float) someFloat();
        if (double.class.equals(type) || Double.class.equals(type)) return () -> (T) (Double) someDouble();
        if (BigDecimal.class.equals(type)) return () -> (T) BigDecimal.valueOf(someInt());

        if (String.class.equals(type)) return () -> (T) someString(some, location);
        if (UUID.class.equals(type)) return () -> (T) someUUID();
        if (URI.class.equals(type)) return () -> (T) someURI();
        if (URL.class.equals(type)) return () -> (T) someURL();

        if (Instant.class.equals(type)) return () -> (T) someInstant();
        if (LocalDate.class.equals(type)) return () -> (T) someLocalDate();
        if (LocalDateTime.class.equals(type)) return () -> (T) someLocalDateTime();
        if (ZonedDateTime.class.equals(type)) return () -> (T) someZonedDateTime();
        if (OffsetDateTime.class.equals(type)) return () -> (T) someOffsetDateTime();
        if (LocalTime.class.equals(type)) return () -> (T) someLocalTime();
        if (OffsetTime.class.equals(type)) return () -> (T) someOffsetTime();
        if (Duration.class.equals(type)) return () -> (T) someDuration();
        if (Period.class.equals(type)) return () -> (T) somePeriod();

        return null;
    }

    static byte someByte() {
        var value = someInt();
        if ((byte) value != value) throw new ArithmeticException("byte overflow");
        return (byte) value;
    }

    static char someChar() {return Character.toChars(someInt())[0];}

    static short someShort() {return (short) someInt();}

    static int someInt() {
        if (nextInt >= Short.MAX_VALUE)
            throw new IllegalStateException("too many values generated (we want to prevent a overflow causing non-unique value)");
        return nextInt++;
    }

    static long someLong() {return someInt();}

    static float someFloat() {return Float.parseFloat(someInt() + ".1");}

    static double someDouble() {return Double.parseDouble(someInt() + ".2");}

    static String someString(Some some, AnnotatedElement location) {
        String tags = (some == null || some.value().length == 0) ? "" : (String.join("-", some.value()) + "-");
        return String.format("%s%s-%05d", tags, name(location), someInt());
    }

    static UUID someUUID() {return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", someInt()));}

    static URI someURI() {return URI.create(String.format("https://example.nowhere/path-%07d", someInt()));}

    @SneakyThrows(MalformedURLException.class)
    static URL someURL() {return someURI().toURL();}

    static Instant someInstant() {return START_INSTANT.plusSeconds(someInt());}

    static LocalDate someLocalDate() {return LocalDate.ofInstant(START_INSTANT, ZoneId.systemDefault()).plusDays(someInt());}

    static LocalDateTime someLocalDateTime() {return LocalDateTime.ofInstant(START_INSTANT, ZoneId.systemDefault()).plusHours(someInt());}

    static ZonedDateTime someZonedDateTime() {return ZonedDateTime.ofInstant(START_INSTANT, ZoneId.systemDefault()).plusMinutes(someInt());}

    static OffsetDateTime someOffsetDateTime() {return OffsetDateTime.ofInstant(START_INSTANT, ZoneId.systemDefault()).plusDays(someInt());}

    static LocalTime someLocalTime() {return LocalTime.ofInstant(START_INSTANT, ZoneId.systemDefault()).plusSeconds(someInt());}

    static OffsetTime someOffsetTime() {return OffsetTime.ofInstant(START_INSTANT, ZoneId.systemDefault()).plusSeconds(someInt());}

    static Duration someDuration() {return Duration.ofSeconds(someInt());}

    static Period somePeriod() {return Period.ofDays(someInt());}
}
