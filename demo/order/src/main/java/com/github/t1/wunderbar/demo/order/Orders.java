package com.github.t1.wunderbar.demo.order;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Slf4j
@GraphQLApi
@Path("/orders")
public class Orders {
    public static final String PRODUCT_ID = "existing-product-id";

    private static final List<Order> ORDERS = List.of(
        Order.builder()
            .id("1")
            .orderDate(LocalDate.of(2021, 12, 31))
            .item(OrderItem.builder().position(1).productId(PRODUCT_ID).build())
            .item(OrderItem.builder().position(2).productId(PRODUCT_ID).build())
            .build(),
        Order.builder()
            .id("2")
            .orderDate(LocalDate.of(2022, 1, 23))
            .item(OrderItem.builder().position(1).productId("p1").build())
            .item(OrderItem.builder().position(2).productId("p2").build())
            .build()
    );
    private static final Map<String, Order> ORDER_MAP = ORDERS.stream().collect(Collectors.toMap(Order::getId, identity()));

    @GET @Path("/{id}")
    @Query public Order order(@NonNull @PathParam("id") String id) {
        log.debug("get order with id {}", id);
        var order = ORDER_MAP.get(id);
        if (order == null) throw new NotFoundException("no order with id " + id);
        log.debug("found order {}", order);
        return order;
    }
}
