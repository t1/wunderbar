package test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.core.MediaType;
import java.util.stream.Stream;

import static com.github.t1.wunderbar.common.Utils.isCompatible;
import static org.assertj.core.api.BDDAssertions.then;

class UtilsTest {
    @ParameterizedTest
    @MethodSource("compatible")
    void shouldBeCompatible(String left, String right) {
        then(isCompatible(mediaType(left), mediaType(right)))
            .describedAs("Content-Type: " + left + " to be compatible to " + right)
            .isTrue();
        then(isCompatible(mediaType(right), mediaType(left)))
            .describedAs("Content-Type: " + right + " to be compatible to " + left)
            .isTrue();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> compatible() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of("*/*", "application/json"),
            Arguments.of("*/json", "application/xml"), // esoteric, but this is how MimeType#isCompatible works
            Arguments.of("application/*", "application/json"),
            Arguments.of("application/json", "application/json"),
            Arguments.of("application/json", "application/json;charset=utf-8"),
            Arguments.of("application/json", "application/graphql+json"),
            Arguments.of("application/some+json", "application/graphql+json")
        );
    }

    @ParameterizedTest
    @MethodSource("incompatible")
    void shouldBeIncompatible(String left, String right) {
        then(isCompatible(mediaType(left), mediaType(right)))
            .describedAs("Content-Type: " + left + " to be incompatible to " + right)
            .isFalse();
        then(isCompatible(mediaType(right), mediaType(left)))
            .describedAs("Content-Type: " + right + " to be incompatible to " + left)
            .isFalse();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> incompatible() {
        return Stream.of(
            Arguments.of("application/json", "application/xml"),
            Arguments.of(null, "application/json"),
            Arguments.of("application/some+json", "application/some+xml")
        );
    }

    private static MediaType mediaType(String string) {
        return (string == null) ? null : MediaType.valueOf(string);
    }
}
