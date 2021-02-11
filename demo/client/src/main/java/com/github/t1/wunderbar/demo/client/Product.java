package com.github.t1.wunderbar.demo.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

import static lombok.AccessLevel.PRIVATE;

@Getter @Setter @ToString @NoArgsConstructor
@Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)
public class Product {
    @Id @NonNull String id;
    String name;
}
