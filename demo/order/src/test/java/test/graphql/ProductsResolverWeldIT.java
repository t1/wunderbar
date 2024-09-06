package test.graphql;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsResolver;
import com.github.t1.wunderbar.demo.order.ProductsResolver.Products;
import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.GraphQLClientException;
import jakarta.inject.Inject;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.ExplicitParamInjection;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import test.SomeProductIds;
import test.SomeProducts;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer(fileName = "target/weld-wunder.jar")
@EnableWeld
@Register({SomeProducts.class, SomeProductIds.class})
@ExplicitParamInjection
class ProductsResolverWeldIT {
    @Service Products products;
    @Inject ProductsResolver resolver;

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ProductsResolver.class, Products.class)
            .addBeans(MockBean.builder().types(Products.class).create(ctx -> products).build())
            .build();

    @Test void shouldResolveProduct(@Some Product product) {
        given(products.product(product.getId())).returns(product);

        var resolvedProduct = resolver.product(item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    @Test void shouldResolveTwoProducts(@Some Product givenProduct1, @Some Product givenProduct2) {
        given(products.product(givenProduct1.getId())).returns(givenProduct1);
        given(products.product(givenProduct2.getId())).returns(givenProduct2);

        var resolvedProduct1 = resolver.product(item(givenProduct1.getId()));
        var resolvedProduct2 = resolver.product(item(givenProduct2.getId()));

        then(resolvedProduct1).usingRecursiveComparison().isEqualTo(givenProduct1);
        then(resolvedProduct2).usingRecursiveComparison().isEqualTo(givenProduct2);
    }

    /** before you mutate an existing object, make sure it exists in the unmodified state */
    @Test void shouldUpdateExistingProductPrice(@Some Product product) {
        given(products.product(product.getId())).returns(product);
        given(products.update(new Product().withId(product.getId()).withPrice(12_99))).returns(product.withPrice(12_99));

        var item = item(product.getId());
        var preCheck = resolver.product(item);
        var resolvedProduct = resolver.productWithPriceUpdate(item, 12_99);

        then(preCheck).usingRecursiveComparison().isEqualTo(product);
        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product.withPrice(12_99));
    }

    @Test void shouldFailToResolveUnknownProduct(@Some("product-id") String id) {
        given(products.product(id)).willThrow(new ProductNotFoundException(id));

        var throwable = catchThrowableOfType(GraphQLClientException.class, () -> resolver.product(item(id)));

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product " + id + " not found");
        then(error.getCode()).isEqualTo("product-not-found");
    }

    @Test void shouldFailToResolveForbiddenProduct(@Some("product-id") String id) {
        given(products.product(id)).willThrow(new ProductForbiddenException(id));

        var throwable = catchThrowableOfType(GraphQLClientException.class, () -> resolver.product(item(id)));

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product " + id + " is forbidden");
        then(error.getCode()).isEqualTo("product-forbidden");
    }

    private OrderItem item(String id) {
        return OrderItem.builder().productId(id).build();
    }

    private static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String id) {super("product " + id + " not found");}
    }

    private static class ProductForbiddenException extends RuntimeException {
        public ProductForbiddenException(String id) {super("product " + id + " is forbidden");}
    }
}
