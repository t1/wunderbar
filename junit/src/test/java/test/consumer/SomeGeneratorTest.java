package test.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SomeBasics;
import com.github.t1.wunderbar.junit.consumer.SomeData;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import com.github.t1.wunderbar.junit.consumer.SomeSingleTypes;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.t1.wunderbar.common.Utils.name;
import static com.github.t1.wunderbar.junit.consumer.SomeBasics.START_INSTANT;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static test.consumer.SomeGeneratorTest.SomeCustomGenerics;
import static test.consumer.SomeGeneratorTest.SomeCustomTypes;
import static test.consumer.SomeGeneratorTest.SomeCustomWrappers;

/** the fact that @Register is @Inherited is tested by the subclasses of the ProductResolverTest */
@WunderBarApiConsumer
@Register({SomeCustomTypes.class, SomeCustomWrappers.class, SomeCustomGenerics.class})
class SomeGeneratorTest {
    private static final int QUITE_SMALL_INT = SomeBasics.DEFAULT_START;
    private static final int QUITE_BIG_INT = Short.MAX_VALUE;

    @Order(2) @Some int intField1;
    @Order(1) @Some int intField2;
    @Order(3) @Some int intField3;
    @Some("value=3") CustomWrapper customWrapper;

    @Test void shouldGenerateFieldsInOrder() {
        then(intField2).isEqualTo(100);
        then(intField1).isEqualTo(101);
        then(intField3).isEqualTo(102);
    }

    @Test void shouldGenerateTaggedField() {
        then(customWrapper).isEqualTo(new CustomWrapper(3));
    }

    @Test void shouldGeneratePrimitiveByte(@Some byte i) {then(i).isBetween((byte) QUITE_SMALL_INT, Byte.MAX_VALUE);}

    @Test void shouldGenerateByte(@Some Byte i) {then(i).isBetween((byte) QUITE_SMALL_INT, Byte.MAX_VALUE);}

    @Test void shouldFailToGenerateLargeByte(SomeGenerator gen) {
        SomeBasics.reset(Byte.MAX_VALUE);
        gen.generate(byte.class);

        var throwable = catchThrowable(() -> gen.generate(byte.class));

        then(throwable).isInstanceOf(ArithmeticException.class)
            .hasMessageContaining("byte overflow");
    }

    @Test void shouldGenerateChar(@Some char i) {then(i).isBetween((char) QUITE_SMALL_INT, Character.MAX_VALUE);}

    @Test void shouldGenerateCharacter(@Some Character i) {then(i).isBetween((char) QUITE_SMALL_INT, Character.MAX_VALUE);}

    @Test void shouldFailToGenerateLargeChar(SomeGenerator gen) {
        SomeBasics.reset(Character.MAX_VALUE);

        var throwable = catchThrowable(() -> gen.generate(char.class));

        then(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("too many values generated");
    }

    @Test void shouldGenerateShort(@Some short i) {then(i).isBetween((short) QUITE_SMALL_INT, (short) QUITE_BIG_INT);}

    @Test void shouldFailToGenerateLargeShort(SomeGenerator gen) {
        SomeBasics.reset(Integer.MAX_VALUE - 100);

        var throwable = catchThrowable(() -> gen.generate(short.class));

        then(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("too many values generated");
    }

    @Test void shouldGenerateInt(@Some int i) {then(i).isBetween(QUITE_SMALL_INT, QUITE_BIG_INT);}

    @Test void shouldGenerateSomeIntegers(@Some int i1, @Some int i2, @Some int i3, @Some int i4, @Some int i5) {
        Stream.of(i1, i2, i3, i4, i5).forEach(i ->
            then(i).isBetween(QUITE_SMALL_INT, QUITE_BIG_INT));
    }

    @Test void shouldFailToGenerateLargeInt(SomeGenerator gen) {
        SomeBasics.reset(Integer.MAX_VALUE - 100);

        var throwable = catchThrowable(() -> gen.generate(int.class));

        then(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("too many values generated");
    }

    @Test void shouldGenerateLong(@Some long i) {then(i).isBetween((long) QUITE_SMALL_INT, (long) QUITE_BIG_INT);}

    @Test void shouldGenerateBigInteger(@Some BigInteger i) {then(i).isBetween(BigInteger.ZERO, BigInteger.valueOf(QUITE_BIG_INT));}

    @Test void shouldGenerateFloat(@Some float i) {then(i).isBetween((float) QUITE_SMALL_INT, (float) QUITE_BIG_INT);}

    @Test void shouldGenerateDouble(@Some double i) {then(i).isBetween((double) QUITE_SMALL_INT, (double) QUITE_BIG_INT);}

    @Test void shouldGenerateBigDecimal(@Some BigDecimal i) {then(i).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(QUITE_BIG_INT));}

    @Test void shouldGenerateString(@Some String string) {then(string).isBetween("string-00000", "string-99999");}

    @Test void shouldGenerateUUID(@Some UUID uuid) {
        then(uuid).isBetween(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000099999"));
    }

    @Test void shouldGenerateURI(@Some URI uri) {
        then(uri.toString()).isBetween("https://example.nowhere/path-0000000", "https://example.nowhere/path-99999");
    }

    @Test void shouldGenerateMediaType(@Some MediaType mediaType) {
        then(mediaType.toString()).isBetween("application/vnd.00000+json;charset=utf-8", "application/vnd.99999+json;charset=utf-8");
    }

    @Test void shouldGenerateURL(@Some URL url) {
        then(url.toString()).isBetween("https://example.nowhere/path-0000000", "https://example.nowhere/path-99999");
    }

    @Test void shouldGenerateInstant(@Some Instant instant) {then(instant).isBetween(START_INSTANT, START_INSTANT.plusSeconds(QUITE_BIG_INT));}

    @Test void shouldGenerateLocalDate(@Some LocalDate localDate) {
        var startLocalDate = LocalDate.ofInstant(START_INSTANT, ZoneId.systemDefault());
        then(localDate).isBetween(startLocalDate, startLocalDate.plusDays(QUITE_BIG_INT));
    }

    @Test void shouldGenerateLocalDateTime(@Some LocalDateTime localDateTime) {
        var startLocalDateTime = LocalDateTime.ofInstant(START_INSTANT, ZoneId.systemDefault());
        then(localDateTime).isBetween(startLocalDateTime, startLocalDateTime.plusHours(QUITE_BIG_INT));
    }

    @Test void shouldGenerateZonedDateTime(@Some ZonedDateTime zonedDateTime) {
        var startZonedDateTime = ZonedDateTime.ofInstant(START_INSTANT, ZoneId.systemDefault());
        then(zonedDateTime).isBetween(startZonedDateTime, startZonedDateTime.plusMinutes(QUITE_BIG_INT));
    }

    @Test void shouldGenerateOffsetDateTime(@Some OffsetDateTime zonedDateTime) {
        var startOffsetDateTime = OffsetDateTime.ofInstant(START_INSTANT, ZoneId.systemDefault());
        then(zonedDateTime).isBetween(startOffsetDateTime, startOffsetDateTime.plusDays(QUITE_BIG_INT));
    }

    @Test void shouldGenerateLocalTime(@Some LocalTime localTime) {
        var startLocalTime = LocalTime.ofInstant(START_INSTANT, ZoneId.systemDefault());
        then(localTime).isBetween(startLocalTime, startLocalTime.plusSeconds(QUITE_BIG_INT));
    }

    @Test void shouldGenerateOffsetTime(@Some OffsetTime localTime) {
        var startOffsetTime = OffsetTime.ofInstant(START_INSTANT, ZoneId.systemDefault());
        then(localTime).isBetween(startOffsetTime, startOffsetTime.plusSeconds(QUITE_BIG_INT));
    }

    @Test void shouldGenerateDuration(@Some Duration duration) {then(duration).isBetween(Duration.ZERO, Duration.ofSeconds(QUITE_BIG_INT));}

    // remember: period is not comparable!
    @Test void shouldGeneratePeriod(@Some Period period) {then(period.getDays()).isBetween(0, QUITE_BIG_INT);}

    // ---------------------------------------------------- custom generators
    @Register(SomeCustomUris.class)
    @Test void shouldGenerateCustomURI(@Some URI uri) {then(uri).hasToString("dummy-uri");}

    static class SomeCustomUris extends SomeSingleTypes<URI> {
        @Override public URI some(Some some, Type type, AnnotatedElement location) {return URI.create("dummy-uri");}
    }

    @Register(SomeCustomIds.class)
    @Test void shouldGenerateCustomId(@Some({"id", "unrelated"}) String id) {then(id).hasToString("custom-id");}

    static class SomeCustomIds extends SomeSingleTypes<@Some("id") String> {
        @Override public String some(Some some, Type type, AnnotatedElement location) {return "custom-id";}
    }

    @Register(SomeCustomPrimitiveIntegers.class)
    @Test void shouldGenerateCustomInt(@Some int i1, @Some Integer i2) {
        then(i1).isBetween(-100, -10);
        then(i2).isEqualTo(100);
    }

    static class SomeCustomPrimitiveIntegers implements SomeData {
        private int nextInt = -100;

        @Override public boolean canGenerate(Some some, Type type, AnnotatedElement location) {
            return int.class.equals(type); // not Integer here
        }

        @SuppressWarnings("unchecked")
        @Override public Integer some(Some some, Type type, AnnotatedElement location) {return nextInt++;}
    }

    @Register(SomeNullStrings.class)
    @Test void shouldFailToGenerateNullStringWithoutLocation(SomeGenerator generator) {
        var throwable = catchThrowable(() -> generator.generate(String.class));

        then(throwable).isInstanceOf(WunderBarException.class).hasMessage("[null-generator] generated a null value");
    }

    @Register(SomeNullStrings.class)
    @Test void shouldFailToGenerateNullStringWithLocation(SomeGenerator generator) {
        var throwable = catchThrowable(() -> generator.generate(Some.LITERAL, String.class, SomeGeneratorTest.class));

        then(throwable).isInstanceOf(WunderBarException.class).hasMessage("[null-generator] generated a null value for " + SomeGeneratorTest.class);
    }

    static class SomeNullStrings extends SomeSingleTypes<String> {
        @Override public String toString() {return "null-generator";}

        @Override public String some(Some some, Type type, AnnotatedElement location) {return null;}
    }

    @Register(SomeFixedStrings.class)
    @Test void shouldFailToGenerateDuplicateString(SomeGenerator generator) {
        generator.generate(String.class);

        var throwable = catchThrowable(() -> generator.generate(String.class));

        then(throwable).isInstanceOf(WunderBarException.class)
            .hasMessage("[fixed-generator] generated a non-unique value for null. There's already \"foo\" -> Some.SomeLiteral(value=[])");
    }

    static class SomeFixedStrings extends SomeSingleTypes<String> {
        @Override public String toString() {return "fixed-generator";}

        @Override public String some(Some some, Type type, AnnotatedElement location) {return "foo";}
    }

    @Nested class WithCustomStart {
        @BeforeEach void initCounter() {SomeBasics.reset(200);}

        @Test void shouldGenerateLowInt(@Some int custom) {then(custom).isEqualTo(200);}
    }

    @Register(SomeNestedIntegers.class)
    @Nested class WithNestedGenerator {
        @Test void shouldGenerateCustomInt(@Some Integer custom) {then(custom).isEqualTo(-1);}
    }

    static class SomeNestedIntegers extends SomeSingleTypes<Integer> {
        @Override public Integer some(Some some, Type type, AnnotatedElement location) {
            return -1;
        }
    }

    // ---------------------------------------------------- custom types
    @Test void shouldGenerateCustomData(@Some("valid") CustomType data, SomeGenerator generator) throws Exception {
        then(generator.location(data)).isEqualTo(SomeGeneratorTest.class.getDeclaredMethod("shouldGenerateCustomData", CustomType.class, SomeGenerator.class).getParameters()[0]);
        then(generator.findSomeFor(data).value()).containsExactly("valid");

        then(data.foo).startsWith("valid-data-foo-");
        then(data.bar).isEqualTo(new CustomWrapper(1));
        then(data.baz).startsWith("baz-cool-");
        then(data.gen).isEqualTo(new CustomGeneric<>(data.gen.wrapped));
        then(generator.location(data.foo.substring("valid-data-".length()))).isEqualTo(CustomType.class.getDeclaredField("foo"));
        then(generator.location(new CustomWrapper(1))).isEqualTo(CustomType.class.getDeclaredField("bar"));
        then(generator.location(data.gen.wrapped)).isEqualTo(CustomGeneric.class.getDeclaredField("wrapped"));
    }

    @Test void shouldFailToFindForeignValue(SomeGenerator generator) {
        var uuid = UUID.randomUUID();

        var throwable = catchThrowable(() -> generator.location(uuid));

        then(throwable).isInstanceOf(WunderBarException.class).hasMessage("this value was not generated via the WunderBar @Some annotation: " + uuid);
    }

    public static @Data @Builder class CustomType {
        String foo;
        CustomWrapper bar;
        @Some("cool") String baz;
        CustomGeneric<String> gen;
    }

    public static @Data @Builder class CustomWrapper {
        int wrapped;
    }

    public static @Data @Builder class CustomGeneric<T> {
        T wrapped;
    }

    @RequiredArgsConstructor
    static class SomeCustomTypes extends SomeSingleTypes<CustomType> {
        private final SomeGenerator generator;

        @Override public CustomType some(Some some, Type type, AnnotatedElement location) {
            assert some.value().length == 1;
            var tag = some.value()[0];
            return CustomType.builder()
                .foo(tag + "-" + name(location) + "-" +
                     generator.generate(CustomType.class, "foo"))
                .bar(generator.generate(CustomType.class, "bar"))
                .baz(generator.generate(CustomType.class, "baz"))
                .gen(generator.generate(CustomType.class, "gen"))
                .build();
        }
    }

    static class SomeCustomWrappers extends SomeSingleTypes<CustomWrapper> {
        @Override public CustomWrapper some(Some some, Type type, AnnotatedElement location) {
            var wrapped = (some == null || some.value().length == 0) ? 1 : from(some.value()[0]);
            return CustomWrapper.builder().wrapped(wrapped).build();
        }

        private int from(String tag) {
            var matcher = Pattern.compile("value=(\\d+)").matcher(tag);
            if (!matcher.matches()) throw new IllegalArgumentException("invalid @Some tag for Custom: " + tag);
            return Integer.parseInt(matcher.group(1));
        }
    }

    @RequiredArgsConstructor
    static class SomeCustomGenerics extends SomeSingleTypes<CustomGeneric<?>> {
        private final SomeGenerator generator;

        @SneakyThrows(ReflectiveOperationException.class)
        @Override public CustomGeneric<?> some(Some some, Type type, AnnotatedElement location) {
            var nestedType = ((ParameterizedType) type).getActualTypeArguments()[0];
            var wrapped = generator.generate(Some.LITERAL, nestedType, CustomGeneric.class.getDeclaredField("wrapped"));
            return CustomGeneric.builder().wrapped(wrapped).build();
        }
    }

    // ---------------------------------------------------- infinite loop
    @Register(SomeInfiniteLoops.class)
    @Test void shouldFailToGenerateInfiniteLoop(SomeGenerator generator) {
        var throwable = catchThrowable(() -> generator.generate(InfiniteLoop.class));

        then(throwable).isInstanceOf(WunderBarException.class).hasMessageContaining("infinite loop");
    }

    private static @Data @Builder class InfiniteLoop {
        String dummy;
    }

    @RequiredArgsConstructor
    static class SomeInfiniteLoops extends SomeSingleTypes<InfiniteLoop> {
        private final SomeGenerator generator;

        @Override public InfiniteLoop some(Some some, Type type, AnnotatedElement location) {
            return generator.generate(InfiniteLoop.class);
        }
    }
}
