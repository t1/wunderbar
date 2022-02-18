package test.graphql;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsResolver;
import com.github.t1.wunderbar.demo.order.ProductsResolver.Products;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static test.SomeProducts.someProduct;

@ExtendWith(MockitoExtension.class)
class ProductsResolverUnitTest {
    @Mock Products products;
    @InjectMocks ProductsResolver resolver;

    Product product = someProduct("123");
    Product other = someProduct("456");

    @Test void shouldResolveProduct() {
        given(products.product(product.getId())).willReturn(product);

        var resolvedProduct = resolver.product(item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldResolveTwoProducts() {
        given(products.product(product.getId())).willReturn(product);
        given(products.product(other.getId())).willReturn(other);

        var resolvedProduct1 = resolver.product(item(product.getId()));
        var resolvedProduct2 = resolver.product(item(other.getId()));

        then(resolvedProduct1).usingRecursiveComparison().isEqualTo(product);
        then(resolvedProduct2).usingRecursiveComparison().isEqualTo(other);
    }

    @Test void shouldFailToResolveUnknownProduct() {
        var id = "12";
        given(products.product(id)).willThrow(new ProductNotFoundException(id));

        var throwable = catchThrowableOfType(() -> resolver.product(item(id)), ProductNotFoundException.class);

        then(throwable).hasMessage("product " + id + " not found");
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        var id = "789";
        given(products.product(id)).willThrow(new ProductForbiddenException(id));

        var throwable = catchThrowableOfType(() -> resolver.product(item(id)), ProductForbiddenException.class);

        then(throwable).hasMessage("product " + 789 + " is forbidden");
    }

    private OrderItem item(String id) {return OrderItem.builder().productId(id).build();}

    private static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String id) {super("product " + id + " not found");}
    }

    private static class ProductForbiddenException extends RuntimeException {
        public ProductForbiddenException(String id) {super("product " + id + " is forbidden");}
    }
}
