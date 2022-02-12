package test.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SomeBasics;
import com.github.t1.wunderbar.junit.consumer.SomeData;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import com.github.t1.wunderbar.junit.consumer.SomeSingleTypeData;
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
import java.util.stream.Stream;

import static com.github.t1.wunderbar.common.Utils.name;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static test.consumer.SomeGeneratorTest.CustomDataGenerator;
import static test.consumer.SomeGeneratorTest.CustomGenerator;
import static test.consumer.SomeGeneratorTest.CustomGenericGenerator;

/** the fact that @Register is @Inherited is tested by the subclasses of the ProductResolverTest */
@WunderBarApiConsumer
@Register({CustomDataGenerator.class, CustomGenerator.class, CustomGenericGenerator.class})
class SomeGeneratorTest {
    private static final int QUITE_SMALL_INT = SomeBasics.DEFAULT_START;
    private static final int QUITE_BIG_INT = Short.MAX_VALUE;

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

    @Test void shouldGenerateSomeIntegers(@Some int i1, @Some int i2, @Some int i3, @Some int i4, @Some int i5) {
        Stream.of(i1, i2, i3, i4, i5).forEach(i ->
            then(i).isBetween(QUITE_SMALL_INT, QUITE_BIG_INT));
    }

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

    @Register(CustomURI.class)
    @Test void shouldGenerateCustomURL(@Some URI uri) {then(uri).hasToString("dummy-uri");}

    static class CustomURI extends SomeSingleTypeData<URI> {
        @Override public URI some(Some some, Type type, AnnotatedElement location) {
            return URI.create("dummy-uri");
        }
    }

    @Register(CustomInt.class)
    @Test void shouldGenerateCustomInt(@Some int i1, @Some Integer i2) {
        then(i1).isEqualTo(-100);
        then(i2).isEqualTo(1000);
    }

    static class CustomInt implements SomeData {
        @Override public boolean canGenerate(Some some, Type type, AnnotatedElement location) {
            return int.class.equals(type); // not Integer here
        }

        @SuppressWarnings("unchecked")
        @Override public Integer some(Some some, Type type, AnnotatedElement location) {return -100;}
    }

    @Nested class WithCustomStart {
        @BeforeEach void initCounter() {SomeBasics.reset(100);}

        @Test void shouldGenerateLowInt(@Some int custom) {then(custom).isEqualTo(100);}
    }

    @Register(NestedGenerator.class)
    @Nested class WithNestedGenerator {
        @Test void shouldGenerateCustomInt(@Some Integer custom) {then(custom).isEqualTo(-1);}
    }

    static class NestedGenerator extends SomeSingleTypeData<Integer> {
        @Override public Integer some(Some some, Type type, AnnotatedElement location) {
            return -1;
        }
    }

    // ---------------------------------------------------- custom data
    @Test void shouldGenerateCustomData(@Some("valid") CustomData data, SomeGenerator generator) throws Exception {
        then(generator.location(data)).isEqualTo(SomeGeneratorTest.class.getDeclaredMethod("shouldGenerateCustomData", CustomData.class, SomeGenerator.class).getParameters()[0]);
        then(generator.findSomeFor(data).value()).containsExactly("valid");

        then(data).hasToString("SomeGeneratorTest.CustomData(foo=valid-data-string-01000, bar=SomeGeneratorTest.Custom(wrapped=1), " +
                               "generic=SomeGeneratorTest.CustomGeneric(wrapped=string-01001))");

        then(generator.location(new Custom(1))).isEqualTo(CustomData.class.getDeclaredField("bar"));
        then(generator.location("string-01001")).isEqualTo(CustomGeneric.class.getDeclaredField("wrapped"));
    }

    @Test void shouldFailToGenerateNullValue(SomeGenerator generator) {
        var throwable = catchThrowable(() -> generator.generate(Some.LITERAL.withTags("generate-null"), CustomData.class, null));

        then(throwable).isInstanceOf(WunderBarException.class).hasMessageStartingWith("the generator generated a null value: " + CustomDataGenerator.class.getName());
    }

    @Test void shouldFailToFindForeignValue(SomeGenerator generator) {
        var uuid = UUID.randomUUID();

        var throwable = catchThrowable(() -> generator.location(uuid));

        then(throwable).isInstanceOf(WunderBarException.class).hasMessage("this value was not generated via the WunderBar @Some annotation: " + uuid);
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
    static class CustomDataGenerator extends SomeSingleTypeData<CustomData> {
        private final SomeGenerator generator;

        @Override public CustomData some(Some some, Type type, AnnotatedElement location) {
            assert some.value().length == 1;
            var tag = some.value()[0];
            if ("generate-null".equals(tag)) return null;
            return CustomData.builder()
                .foo(tag + "-" + name(location) + "-" + generator.generate(CustomData.class, "foo"))
                .bar(generator.generate(CustomData.class, "bar"))
                .generic(generator.generate(CustomData.class, "generic"))
                .build();
        }
    }

    static class CustomGenerator extends SomeSingleTypeData<Custom> {
        @Override public Custom some(Some some, Type type, AnnotatedElement location) {
            return Custom.builder().wrapped(1).build();
        }
    }

    @RequiredArgsConstructor
    static class CustomGenericGenerator extends SomeSingleTypeData<CustomGeneric<?>> {
        private final SomeGenerator generator;

        @SneakyThrows(ReflectiveOperationException.class)
        @Override public CustomGeneric<?> some(Some some, Type type, AnnotatedElement location) {
            var nestedType = ((ParameterizedType) type).getActualTypeArguments()[0];
            var wrapped = generator.generate(Some.LITERAL, nestedType, CustomGeneric.class.getDeclaredField("wrapped"));
            return CustomGeneric.builder().wrapped(wrapped).build();
        }
    }

    // ---------------------------------------------------- infinite loop
    @Register(InfiniteLoopGenerator.class)
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
    static class InfiniteLoopGenerator extends SomeSingleTypeData<InfiniteLoop> {
        private final SomeGenerator generator;

        @Override public InfiniteLoop some(Some some, Type type, AnnotatedElement location) {
            return generator.generate(InfiniteLoop.class);
        }
    }
}
