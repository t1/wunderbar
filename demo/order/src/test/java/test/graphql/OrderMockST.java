package test.graphql;

import com.github.t1.testcontainers.jee.JeeContainer;
import com.github.t1.testcontainers.jee.WildflyContainer;
import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import com.github.t1.wunderbar.junit.consumer.SomeSingleTypeData;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import test.Slow;
import test.SomeProductId;
import test.graphql.OrderMockST.SomeMockProduct;
import test.graphql.OrderMockST.SomeOrderInput;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

import static com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer.NONE;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.BDDAssertions.then;
import static org.testcontainers.containers.Network.newNetwork;

@Slow
@Testcontainers
@WunderBarApiConsumer(fileName = NONE)
@Register({SomeProductId.class, SomeMockProduct.class, SomeOrderInput.class})
class OrderMockST {
    private static final Network NETWORK = newNetwork();

    @Container static JeeContainer ORDERS = jeeContainer()
        .withMainPortBoundToFixedPort(18080)
        .withPortBoundToFixedPort(19990, 9990)
        .withPortBoundToFixedPort(18787, 8787)
        .withDeployment("target/order.war");

    @Container static JeeContainer PRODUCTS = jeeContainer()
        .withMainPortBoundToFixedPort(28080)
        .withPortBoundToFixedPort(29990, 9990)
        .withPortBoundToFixedPort(28787, 8787)
        .withNetworkAliases("products")
        .withDeployment("../../mock/target/wunderbar-mock-server.war");

    @SuppressWarnings("resource") private static JeeContainer jeeContainer() {
        return new WildflyContainer("rdohna/wildfly", "25.0.1.Final-jdk11").withNetwork(NETWORK);
    }

    interface Api {
        Order order(@NonNull String id);
        @Mutation CreateOrderResponse create(@NonNull OrderInput order);
    }

    @Name("OrderInput")
    @Data @SuperBuilder @NoArgsConstructor
    public static class OrderInput {
        LocalDate orderDate;
        @Singular List<OrderItemInput> items;
    }

    @Name("Order")
    @Data @SuperBuilder @NoArgsConstructor
    public static class CreateOrderResponse {
        String id;
    }

    @Data @SuperBuilder @NoArgsConstructor
    public static class Order {
        String id;
        LocalDate orderDate;
        @Singular List<OrderItem> items;
    }

    @Name("OrderItemInput")
    @Data @SuperBuilder @NoArgsConstructor
    public static class OrderItemInput {
        int position;
        String productId;
    }

    @Data @SuperBuilder @NoArgsConstructor
    public static class OrderItem {
        int position;
        String productId;
        MockProduct product;
    }

    @Name("Product")
    @Data @SuperBuilder @NoArgsConstructor
    public static class MockProduct {
        @Some("product-id") String id;
        String name;
        String description;
        Integer price;
    }

    @GraphQLClientApi
    public interface Products {
        @Query MockProduct product(@NonNull String id);
    }

    @Service(endpoint = "{endpoint()}") Products products;
    Api api = TypesafeGraphQLClientBuilder.newBuilder().endpoint(ORDERS.baseUri() + "graphql").build(Api.class);

    @Some("product-id") static String productId;
    @Some MockProduct product;
    LocalDate orderDate;
    String orderId;

    @SuppressWarnings("unused")
    String endpoint() {return "http://localhost:" + PRODUCTS.getMappedPort(8080) + "/wunderbar-mock-server/graphql";}

    @RequiredArgsConstructor
    static class SomeMockProduct extends SomeSingleTypeData<MockProduct> {
        private final SomeGenerator generator;

        @Override public MockProduct some(Some some, Type type, AnnotatedElement location) {
            return MockProduct.builder()
                .id(productId)
                .name("product #" + productId)
                .description(generator.generate(MockProduct.class, "description"))
                .price(generator.generate(MockProduct.class, "price"))
                .build();
        }
    }

    @RequiredArgsConstructor
    static class SomeOrderInput extends SomeSingleTypeData<OrderInput> {
        private final SomeGenerator generator;

        @Override public OrderInput some(Some some, Type type, AnnotatedElement location) {
            return OrderInput.builder()
                .orderDate(generator.generate(OrderInput.class, "orderDate"))
                .item(OrderItemInput.builder()
                    .position(1)
                    .productId(productId)
                    .build())
                .item(OrderItemInput.builder()
                    .position(2)
                    .productId(productId)
                    .build())
                .build();
        }
    }

    @BeforeEach
    void setUp(@Some OrderInput order) {
        this.orderDate = order.orderDate;
        var created = api.create(order);
        this.orderId = created.id;
    }

    @Test void shouldGetOrder() {
        given(products.product(product.id)).returns(product);

        var actual = api.order(orderId);

        then(actual.id).isEqualTo(orderId);
        then(actual.orderDate).isEqualTo(orderDate);
        then(actual.items).hasSize(2);
        then(actual.items.get(0).position).isEqualTo(1);
        then(actual.items.get(0).product).isEqualTo(product);
        then(actual.items.get(1).position).isEqualTo(2);
        then(actual.items.get(1).product).isEqualTo(product);
    }
}
