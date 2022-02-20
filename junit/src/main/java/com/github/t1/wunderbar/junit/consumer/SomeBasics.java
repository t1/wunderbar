package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MediaType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.github.t1.wunderbar.common.Utils.name;
import static com.github.t1.wunderbar.common.Utils.setField;
import static java.time.ZoneOffset.UTC;

/**
 * Generates random values, but tries to keep them small, positive, and unique, so they are as easy to handle as possible.
 * These prerequisites are not achievable for booleans or null, obviously.
 * Strings, etc., also contain a numeric value to make them unique.
 * The default starting point is 100, so you can use smaller constants in your code without interfering with the generated
 * values. If you need bigger constants, you can change the starting point by calling {@link #reset(int)},
 * e.g. in a {@link org.junit.jupiter.api.BeforeEach} method.
 * <p>
 * Note that the <code>some...</code> methods are public, but the `@Some` annotation comes with superpowers:
 * automatic logging, reset for every test, and most often a location that you can look up.
 */
@Slf4j
@RequiredArgsConstructor
public class SomeBasics implements SomeData {
    public static final int DEFAULT_START = 100;
    public static final Instant START_INSTANT = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, UTC).toInstant();

    private static int nextInt;

    static {reset();}

    private final SomeGenerator someGenerator;

    public static void reset() {reset(DEFAULT_START);}

    public static void reset(int start) {nextInt = start;}

    @Override public <T> Optional<T> some(Some some, Type type, AnnotatedElement location) {
        if (byte.class.equals(type) || Byte.class.equals(type)) return optional(someByte());
        if (char.class.equals(type) || Character.class.equals(type)) return optional(someChar());
        if (short.class.equals(type) || Short.class.equals(type)) return optional(someShort());
        if (int.class.equals(type) || Integer.class.equals(type)) return optional(someInt());
        if (long.class.equals(type) || Long.class.equals(type)) return optional(someLong());
        if (BigInteger.class.equals(type)) return optional(BigInteger.valueOf(someInt()));

        if (float.class.equals(type) || Float.class.equals(type)) return optional(someFloat());
        if (double.class.equals(type) || Double.class.equals(type)) return optional(someDouble());
        if (BigDecimal.class.equals(type)) return optional(BigDecimal.valueOf(someInt()));

        if (String.class.equals(type)) return optional(someString(some, location));
        if (UUID.class.equals(type)) return optional(someUUID());
        if (URI.class.equals(type)) return optional(someURI());
        if (URL.class.equals(type)) return optional(someURL());
        if (MediaType.class.equals(type)) return optional(someMediaType());

        if (Instant.class.equals(type)) return optional(someInstant());
        if (LocalDate.class.equals(type)) return optional(someLocalDate());
        if (LocalDateTime.class.equals(type)) return optional(someLocalDateTime());
        if (ZonedDateTime.class.equals(type)) return optional(someZonedDateTime());
        if (OffsetDateTime.class.equals(type)) return optional(someOffsetDateTime());
        if (LocalTime.class.equals(type)) return optional(someLocalTime());
        if (OffsetTime.class.equals(type)) return optional(someOffsetTime());
        if (Duration.class.equals(type)) return optional(someDuration());
        if (Period.class.equals(type)) return optional(somePeriod());

        if (type instanceof ParameterizedType) {
            var parameterized = (ParameterizedType) type;
            var rawType = parameterized.getRawType();
            if (List.class.equals(rawType))
                return optional(List.of((Object) someGenerator.generate(some, parameterized.getActualTypeArguments()[0], location)));
            if (Set.class.equals(rawType))
                return optional(Set.of((Object) someGenerator.generate(some, parameterized.getActualTypeArguments()[0], location)));
        }

        if (type instanceof Class) {
            try {
                return optional(generate((Class<?>) type));
            } catch (NoSuchMethodException | InstantiationException e) {
                log.debug("can not create {}: {}", type.getTypeName(), e.toString());
                // fall through
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new WunderBarException("failed to generate an instance of " + type.getTypeName(), e);
            }
        }

        return Optional.empty();
    }

    private static <T> Optional<T> optional(Object value) {
        //noinspection unchecked
        return (Optional<T>) Optional.of(value);
    }

    private Object generate(Class<?> classType) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> noArgsConstructor = classType.getDeclaredConstructor();
        noArgsConstructor.setAccessible(true);
        var instance = noArgsConstructor.newInstance();
        for (Class<?> type = classType; type.getSuperclass() != null; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                var value = someGenerator.generate(field);
                setField(instance, field, value);
            }
        }
        return instance;
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
        String tags = (some == null || some.value().length == 0) ? "" : ("-" + String.join("-", some.value()));
        return String.format("%s%s-%05d", name(location), tags, someInt());
    }

    static UUID someUUID() {return UUID.fromString(someFormattedString("00000000-0000-0000-0000-%012d"));}

    static URI someURI() {return URI.create(someFormattedString("https://example.nowhere/path-%07d"));}

    @SneakyThrows(MalformedURLException.class)
    static URL someURL() {return someURI().toURL();}

    static MediaType someMediaType() {return MediaType.valueOf(someFormattedString("application/vnd.%05d+json;charset=utf-8"));}

    static String someFormattedString(String format) {return String.format(format, someInt());}

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
