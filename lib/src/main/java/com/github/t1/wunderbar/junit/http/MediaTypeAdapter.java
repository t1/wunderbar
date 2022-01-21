package com.github.t1.wunderbar.junit.http;

import javax.json.bind.adapter.JsonbAdapter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

@Provider
public class MediaTypeAdapter implements JsonbAdapter<MediaType, String> {
    @Override public String adaptToJson(MediaType obj) {
        return obj.toString();
    }

    @Override public MediaType adaptFromJson(String obj) {
        return MediaType.valueOf(obj);
    }
}
