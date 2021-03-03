package com.github.t1.wunderbar.demo.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

import static lombok.AccessLevel.PRIVATE;

@Getter @Setter @ToString @NoArgsConstructor
@Builder(toBuilder = true) @With @AllArgsConstructor(access = PRIVATE)
public class Product {
    @NonNull @Id String id;
    String name;
    String description;
    Integer price;
}
