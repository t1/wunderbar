package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import static com.github.t1.wunderbar.junit.consumer.SomeBasics.testUriFromInt;
import static com.github.t1.wunderbar.junit.consumer.SomeBasics.testUrlFromInt;
import static com.github.t1.wunderbar.junit.consumer.SomeBasics.testUuidFromInt;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer
class SomeBasicsTest {
    /** We don't want to generate really random numbers; they should be rather small to be easier to handle. */
    private static final int QUITE_BIG_INT = Short.MAX_VALUE;

    @Test void shouldProvidePrimitiveBoolean(@Some boolean b) {then(b).isNotNull();}

    @Test void shouldProvideBigBoolean(@Some Boolean b) {then(b).isNotNull();}

    @Test void shouldProvideShort(@Some short i) {then(i).isBetween((short) 0, (short) QUITE_BIG_INT);}

    @Test void shouldProvideByte(@Some byte i) {then(i).isBetween((byte) 0, Byte.MAX_VALUE);}

    @Test void shouldProvideChar(@Some char i) {then(i).isBetween((char) 0, Character.MAX_VALUE);}

    @Test void shouldProvideCharacter(@Some Character i) {then(i).isBetween((char) 0, Character.MAX_VALUE);}

    @Test void shouldProvideInt(@Some int i) {then(i).isBetween(0, QUITE_BIG_INT);}

    @Test void shouldProvideLong(@Some long i) {then(i).isBetween(0L, (long) QUITE_BIG_INT);}

    @Test void shouldProvideBigInteger(@Some BigInteger i) {then(i).isBetween(BigInteger.ZERO, BigInteger.valueOf(QUITE_BIG_INT));}

    @Test void shouldProvideFloat(@Some float i) {then(i).isBetween(0F, (float) QUITE_BIG_INT);}

    @Test void shouldProvideDouble(@Some double i) {then(i).isBetween(0D, (double) QUITE_BIG_INT);}

    @Test void shouldProvideBigDecimal(@Some BigDecimal i) {then(i).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(QUITE_BIG_INT));}

    @Test void shouldProvideString(@Some String string) {then(string).isNotNull().hasSizeBetween(1, 10);}

    @Test void shouldProvideUUID(@Some UUID uuid) {then(uuid).isBetween(testUuidFromInt(0), testUuidFromInt(QUITE_BIG_INT));}

    @Test void shouldProvideURI(@Some URI uri) {
        then(uri.toString()).isBetween(testUriFromInt(0).toString(), testUriFromInt(QUITE_BIG_INT).toString());
    }

    @Test void shouldProvideURL(@Some URL uri) {
        then(uri.toString()).isBetween(testUrlFromInt(0).toString(), testUrlFromInt(QUITE_BIG_INT).toString());
    }
}
