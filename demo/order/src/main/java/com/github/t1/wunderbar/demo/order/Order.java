package com.github.t1.wunderbar.demo.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

import java.time.LocalDate;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Getter @Setter @ToString @NoArgsConstructor
@Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)
public class Order {
    @NonNull @Id String id;
    @NonNull LocalDate orderDate;
    @NonNull @Singular List<OrderItem> items;
}
