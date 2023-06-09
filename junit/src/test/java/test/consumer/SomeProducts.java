package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import com.github.t1.wunderbar.junit.consumer.SomeSingleTypes;
import lombok.RequiredArgsConstructor;
import test.consumer.ProductResolver.Product;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

@RequiredArgsConstructor
public class SomeProducts extends SomeSingleTypes<Product> {
    private final SomeGenerator generator;

    @Override public Product generate(Some some, Type type, AnnotatedElement location) {
        String id = generator.generate(Some.LITERAL.withTags("id"), String.class, location);
        int price = generator.generate(Product.class, "price");
        return Product.builder().id(id).name("test product " + id).price(price).build();
    }
}
