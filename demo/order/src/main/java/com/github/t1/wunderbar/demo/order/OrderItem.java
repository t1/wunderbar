package com.github.t1.wunderbar.demo.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Id;

import static lombok.AccessLevel.PRIVATE;

@Getter @Setter @ToString @NoArgsConstructor
@Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)
public class OrderItem {
    int position;
    @Id String productId;
}
