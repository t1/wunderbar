package com.github.t1.wunderbar.demo.server;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Id;

import static lombok.AccessLevel.PRIVATE;

@Getter @Setter @ToString @NoArgsConstructor(access = PRIVATE)
@Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)
public class Product {
    @Id String id;
    String name;
}
