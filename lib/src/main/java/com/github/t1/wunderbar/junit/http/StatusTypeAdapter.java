package com.github.t1.wunderbar.junit.http;

import javax.json.bind.adapter.JsonbAdapter;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.Provider;

@Provider
public class StatusTypeAdapter implements JsonbAdapter<StatusType, String> {
    @Override public String adaptToJson(StatusType obj) {
        return obj.toEnum().name();
    }

    @Override public StatusType adaptFromJson(String obj) {
        return Status.valueOf(obj);
    }
}
