package test.consumer;

import com.github.t1.wunderbar.junit.consumer.SomeData;
import test.consumer.ProductResolver.Product;

import static com.github.t1.wunderbar.junit.consumer.SomeBasics.someInt;

public class SomeProduct implements SomeData {
    @Override public <T> T some(Class<T> type) {
        assert type == Product.class;
        return type.cast(someProduct());
    }

    static Product someProduct() {
        var id = someInt();
        return Product.builder().id("#" + id).name("product " + id).price(1000 + id).build();
    }
}
