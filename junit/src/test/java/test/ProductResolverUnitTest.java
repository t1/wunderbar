package test;

import com.github.t1.wunderbar.junit.WunderBarExtension;

import static com.github.t1.wunderbar.junit.Level.UNIT;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarExtension(level = UNIT)
class ProductResolverUnitTest extends ProductResolverTest {
    @Override void failsWith(Throwable throwable, String message) {
        then(throwable).hasMessage(message);
    }
}
