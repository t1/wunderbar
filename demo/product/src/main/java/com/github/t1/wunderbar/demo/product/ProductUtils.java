package com.github.t1.wunderbar.demo.product;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

class ProductUtils {
    private static final Jsonb JSONB = JsonbBuilder.create();

    public static String toString(Object object) {
        return JSONB.toJson(object).replaceAll("\"", "");
    }
}
