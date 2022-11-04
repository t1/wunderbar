package test;

import io.smallrye.graphql.client.GraphQLClientException;
import io.smallrye.graphql.client.impl.GraphQLErrorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.t1.wunderbar.junit.assertions.GraphQLErrorAssert.GRAPHQL_ERROR;
import static com.github.t1.wunderbar.junit.assertions.MediaTypeAssert.MEDIA_TYPE;
import static com.github.t1.wunderbar.junit.assertions.MediaTypeAssert.compatibleTo;
import static com.github.t1.wunderbar.junit.assertions.MediaTypeAssert.isCompatibleToAny;
import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.http.HttpUtils.isCompatible;
import static com.github.t1.wunderbar.junit.http.HttpUtils.mediaTypes;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

class WunderBarAssertionsTest {
    @ParameterizedTest
    @MethodSource("compatible")
    void shouldBeCompatible(String left, String right) {
        var leftTypes = mediaTypes(left);
        var rightTypes = mediaTypes(right);

        switch (leftTypes.size()) {
            case 0:
                then(isCompatible(leftTypes, rightTypes)).isTrue();
                break;
            case 1:
                then(leftTypes.get(0)).isCompatibleTo(rightTypes);
                if (rightTypes.size() == 1) {
                    then(leftTypes.get(0)).isCompatibleTo(rightTypes.get(0));
                }
                then(leftTypes).are(compatibleTo(rightTypes));
                then(leftTypes).singleElement().is(compatibleTo(rightTypes));
                then(leftTypes).singleElement().asInstanceOf(MEDIA_TYPE).isCompatibleTo(rightTypes);
                break;
            default:
                then(leftTypes).anyMatch(type -> isCompatibleToAny(rightTypes, type));
        }

        then(rightTypes).are(compatibleTo(leftTypes));
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
            Arguments.of("application/some+json", "application/graphql+json"),
            Arguments.of("application/json, */*; q=0.5", "application/graphql+json"),
            Arguments.of("application/xml, application/json; q=0.5", "application/graphql+json")
        );
    }

    @ParameterizedTest
    @MethodSource("incompatible")
    void shouldBeIncompatible(String left, String right) {
        then(isCompatible(mediaTypes(left), mediaTypes(right)))
            .describedAs("Content-Type: " + left + " to be incompatible to " + right)
            .isFalse();
        then(isCompatible(mediaTypes(right), mediaTypes(left)))
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

    @Test void shouldParseDoubleMediaType() {
        var mediaType = mediaTypes("application/json, */*; q=0.5");

        then(mediaType).containsExactly(APPLICATION_JSON_TYPE, MediaType.valueOf("*/*;q=0.5"));
    }

    @Test void shouldVerifyGraphQLErrors() {
        var exception = new GraphQLClientException("foobar", List.of(graphQLError("foo"), graphQLError("bar")));

        then(exception).hasErrorCode("foo").withMessageThat().isEqualTo("foo-message");
        then(exception).hasErrorCode("bar")
            .withMessage("bar-message")
            .withMessageContaining("bar")
            .withMessageThat().contains("-");
        Object error = exception.getErrors().get(0);
        then(error).asInstanceOf(GRAPHQL_ERROR).withMessage("foo-message");
    }

    private GraphQLErrorImpl graphQLError(String code) {
        return new GraphQLErrorImpl(code + "-message", null, null, Map.of("code", code), null);
    }
}
