package com.github.t1.wunderbar.demo.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

import static lombok.AccessLevel.PRIVATE;

@Getter @Setter @NoArgsConstructor
@Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)
public class Product {
    @NonNull @Id String id;
    String name;
    String description;
    Integer price;

    @Override public String toString() { return Utils.toString(this); }

    public Product apply(Product patch) {
        if (patch.name != null) this.name = patch.name;
        if (patch.description != null) this.description = patch.description;
        if (patch.price != null) this.price = patch.price;
        return this;
    }
}
