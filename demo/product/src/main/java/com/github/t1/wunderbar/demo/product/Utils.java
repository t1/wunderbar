package com.github.t1.wunderbar.demo.product;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

public class Utils {
    private static final Jsonb JSONB = JsonbBuilder.create();

    public static String toString(Object object) {
        return JSONB.toJson(object).replaceAll("\"", "");
    }
}
