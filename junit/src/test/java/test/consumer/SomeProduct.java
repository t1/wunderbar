package test.consumer;

import com.github.t1.wunderbar.junit.consumer.SomeData;
import test.consumer.ProductResolver.Product;

import static com.github.t1.wunderbar.junit.consumer.SomeBasics.someInt;

public class SomeProduct implements SomeData {
    @Override public boolean canGenerate(Class<?> type) {
        return Product.class.equals(type);
    }

    @Override public <T> T some(Class<T> type) {
        return type.cast(someProduct());
    }

    static Product someProduct() {
        var id = someInt();
        return Product.builder().id("#" + id).name("product " + id).price(1000 + id).build();
    }
}
