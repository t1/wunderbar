package test.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SomeBasics;
import com.github.t1.wunderbar.junit.consumer.SomeData;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import static com.github.t1.wunderbar.common.Utils.name;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer
class SomeBasicsTest {
    /** We don't want to generate really random numbers; they should be rather small to be easier to handle. */
    private static final int QUITE_SMALL_INT = SomeBasics.DEFAULT_START;
    private static final int QUITE_BIG_INT = Short.MAX_VALUE;

    @Register CustomDataGenerator customData;
    @Register CustomGenerator custom;
    @Register CustomGenericGenerator genGen;
    @Register InfiniteLoopGenerator infGen;

    @Test void shouldGenerateChar(@Some char i) {then(i).isBetween((char) QUITE_SMALL_INT, Character.MAX_VALUE);}

    @Test void shouldGenerateCharacter(@Some Character i) {then(i).isBetween((char) QUITE_SMALL_INT, Character.MAX_VALUE);}

    @Test void shouldGenerateShort(@Some short i) {then(i).isBetween((short) QUITE_SMALL_INT, (short) QUITE_BIG_INT);}

    @Test void shouldFailToProvideLargeShort(SomeGenerator gen) {
        SomeBasics.reset(Integer.MAX_VALUE - 100);

        var throwable = catchThrowable(() -> gen.generate(short.class));

        then(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("too many values generated");
    }

    @Test void shouldGenerateInt(@Some int i) {then(i).isBetween(QUITE_SMALL_INT, QUITE_BIG_INT);}

    @Test void shouldFailToProvideLargeInt(SomeGenerator gen) {
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

    @Test void shouldGenerateURL(@Some URL url) {
        then(url.toString()).isBetween("https://example.nowhere/path-0000000", "https://example.nowhere/path-99999");
    }

    @Nested class WithCustomStart {
        @BeforeEach void initCounter() {SomeBasics.reset(100);}

        @Test void shouldGenerateLowInt(@Some int i) {then(i).isEqualTo(100);}
    }


    // ---------------------------------------------------- custom data
    @Test void shouldGenerateCustomData(@Some("valid") CustomData data) {
        then(data).hasToString("SomeBasicsTest.CustomData(foo=valid-data-string-01000, bar=SomeBasicsTest.Custom(wrapped=1), " +
                               "generic=SomeBasicsTest.CustomGeneric(wrapped=string-01001))");
    }

    private static @Data @Builder class CustomData {
        String foo;
        Custom bar;
        CustomGeneric<String> generic;
    }

    private static @Data @Builder class Custom {
        int wrapped;
    }

    private static @Data @Builder class CustomGeneric<T> {
        T wrapped;
    }

    @RequiredArgsConstructor
    private static class CustomDataGenerator implements SomeData {
        private final SomeGenerator generator;

        @Override public boolean canGenerate(Some some, Type type, AnnotatedElement location) {return CustomData.class.equals(type);}

        @SuppressWarnings("unchecked")
        @Override public CustomData some(Some some, Type type, AnnotatedElement location) {
            assert some.value().length == 1;
            var tag = some.value()[0];
            return CustomData.builder()
                .foo(tag + "-" + name(location) + "-" + generator.generate(String.class))
                .bar(generator.generate(CustomData.class, "bar"))
                .generic(generator.generate(CustomData.class, "generic"))
                .build();
        }
    }

    private static class CustomGenerator implements SomeData {
        @Override public boolean canGenerate(Some some, Type type, AnnotatedElement location) {return Custom.class.equals(type);}

        @SuppressWarnings("unchecked")
        @Override public Custom some(Some some, Type type, AnnotatedElement location) {
            return Custom.builder().wrapped(1).build();
        }
    }

    @RequiredArgsConstructor
    private static class CustomGenericGenerator implements SomeData {
        private final SomeGenerator generator;

        @Override public boolean canGenerate(Some some, Type type, AnnotatedElement location) {
            return SomeData.ifParameterized(type, parameterized -> CustomGeneric.class.equals(parameterized.getRawType()));
        }

        @SuppressWarnings("unchecked")
        @SneakyThrows(ReflectiveOperationException.class)
        @Override public CustomGeneric<?> some(Some some, Type type, AnnotatedElement location) {
            var parameterized = (ParameterizedType) type;
            var wrapped = generator.generate(Some.LITERAL, parameterized.getActualTypeArguments()[0], CustomGeneric.class.getDeclaredField("wrapped"));
            return CustomGeneric.builder().wrapped(wrapped).build();
        }
    }

    // ---------------------------------------------------- infinite loop
    @Test void shouldFailToGenerateInfiniteLoop(SomeGenerator generator) {
        var throwable = catchThrowable(() -> generator.generate(InfiniteLoop.class));

        then(throwable).isInstanceOf(WunderBarException.class).hasMessageContaining("infinite loop");
    }

    private static @Data @Builder class InfiniteLoop {
        String foo;
        int bar;
        CustomGeneric<String> generic;
    }

    @RequiredArgsConstructor
    private static class InfiniteLoopGenerator implements SomeData {
        private final SomeGenerator generator;

        @Override public boolean canGenerate(Some some, Type type, AnnotatedElement location) {return InfiniteLoop.class.equals(type);}

        @SuppressWarnings("unchecked")
        @Override public InfiniteLoop some(Some some, Type type, AnnotatedElement location) {
            return generator.generate(InfiniteLoop.class);
        }
    }
}
