package com.github.t1.wunderbar.junit.assertions;

import com.github.t1.wunderbar.junit.http.HttpUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.api.InstanceOfAssertFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;

/**
 * Assertions about a {@link WebApplicationException} that contains a
 * <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC-7807 Problems Detail</a> body.
 */
@SuppressWarnings("UnusedReturnValue")
public class MediaTypeAssert<SELF extends MediaTypeAssert<SELF, ACTUAL>, ACTUAL extends MediaType>
    extends AbstractAssert<SELF, ACTUAL> {
    public static final InstanceOfAssertFactory<MediaType, MediaTypeAssert<?, ?>> MEDIA_TYPE
        = new InstanceOfAssertFactory<>(MediaType.class, MediaTypeAssert::new);

    public static Condition<? super MediaType> compatibleTo(List<MediaType> expectedTypes) {
        return new Condition<>(actual -> isCompatibleToAny(expectedTypes, actual),
            "compatible to any of {}", expectedTypes);
    }

    public static boolean isCompatibleToAny(List<MediaType> expectedTypes, MediaType actual) {
        return expectedTypes.stream().anyMatch(expected -> HttpUtils.isCompatible(actual, expected));
    }

    protected MediaTypeAssert(ACTUAL actual) {this(actual, MediaTypeAssert.class);}

    protected MediaTypeAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

    public SELF isCompatibleTo(MediaType expected) {return isCompatibleTo(List.of(expected));}

    public SELF isCompatibleTo(List<MediaType> expected) {
        then(actual).is(compatibleTo(expected));
        return myself;
    }
}
