package com.github.t1.wunderbar.junit.http;

import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.ext.Provider;

@Provider
public class StatusTypeAdapter implements JsonbAdapter<StatusType, String> {
    @Override public String adaptToJson(StatusType obj) {
        return obj.toEnum().name();
    }

    @Override public StatusType adaptFromJson(String obj) {
        return Status.valueOf(obj);
    }
}
