package test;

import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import com.github.t1.wunderbar.junit.consumer.SomeSingleTypes;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

@RequiredArgsConstructor
public class SomeProducts extends SomeSingleTypes<Product> {
    private final SomeGenerator generator;

    @SneakyThrows(NoSuchFieldException.class)
    @Override public Product generate(Some some, Type type, AnnotatedElement location) {
        var id = (String) generator.generate(Some.LITERAL.withTags("product-id"), String.class, Product.class.getDeclaredField("id"));
        return someProduct(id);
    }

    public static Product someProduct(String id) {
        return Product.builder().id(id).name("product " + id).description("a cool product " + id).price(1599).build();
    }
}
