package test;

import com.github.t1.wunderbar.demo.client.OrderItem;
import com.github.t1.wunderbar.demo.client.Product;
import com.github.t1.wunderbar.demo.client.ProductsGraphQlResolver;
import com.github.t1.wunderbar.demo.client.ProductsGraphQlResolver.Products;
import com.github.t1.wunderbar.junit.Service;
import com.github.t1.wunderbar.junit.SystemUnderTest;
import com.github.t1.wunderbar.junit.WunderBarExtension;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientException;
import org.junit.jupiter.api.Test;

import static com.github.t1.wunderbar.junit.OngoingStubbing.given;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarExtension
class ProductsGraphQlResolverTest {
    @Service Products products;
    @SystemUnderTest ProductsGraphQlResolver resolver;

    @Test void shouldResolveProduct() {
        var givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(products.product(givenProduct.getId())).willReturn(givenProduct);

        var resolvedProduct = resolver.product(OrderItem.builder().productId(givenProduct.getId()).build());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product("x")).willThrow(new RuntimeException("product x not found"));
        var item = OrderItem.builder().productId("x").build();

        var throwable = catchThrowableOfType(() -> resolver.product(item), GraphQlClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product x not found");
    }
}
