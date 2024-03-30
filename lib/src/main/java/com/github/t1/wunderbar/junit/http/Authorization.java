package com.github.t1.wunderbar.junit.http;

import lombok.AllArgsConstructor;
import lombok.Value;

import static com.github.t1.wunderbar.junit.http.HttpUtils.base64;
import static com.github.t1.wunderbar.junit.http.HttpUtils.base64decode;
import static com.github.t1.wunderbar.junit.http.HttpUtils.jwtUpn;
import static java.util.Locale.ROOT;

public interface Authorization {
    static Authorization valueOf(String string) {
        if (string == null) return null;
        var split = string.split(" ", 2);
        if (split.length != 2) throw new IllegalArgumentException("unsupported authorization format: need a type");
        var type = split[0].toLowerCase(ROOT);
        var credentials = split[1];
        return switch (type) {
            case "dummy" -> new Dummy(credentials);
            case "basic" -> new Basic(credentials);
            case "bearer" -> new Bearer(credentials);
            default -> throw new IllegalArgumentException("unsupported authorization type " + type);
        };
    }

    /** The credentials as used for the http <code>Authorization</code> header */
    String toHeader();
    Dummy toDummy();

    @Value class Dummy implements Authorization {
        String username;

        @Override public String toString() {return "Dummy " + username;}

        @Override public String toHeader() {throw new UnsupportedOperationException();}

        @Override public Dummy toDummy() {return this;}
    }

    @AllArgsConstructor
    @Value class Basic implements Authorization {
        String username;
        String password;

        private Basic(String credentials) {
            var split = base64decode(credentials).split(":", 2);
            if (split.length != 2) throw new IllegalArgumentException("invalid basic authorization format");
            this.username = split[0];
            this.password = split[1];
        }

        @Override public String toString() {return "Basic " + username + ":<hidden>";}

        @Override public String toHeader() {return "Basic " + base64(username + ":" + password);}

        @Override public Dummy toDummy() {return new Dummy(username);}
    }

    @Value class Bearer implements Authorization {
        String token;

        @Override public String toString() {return "Bearer token";}

        @Override public String toHeader() {return "Bearer " + token;}

        @Override public Dummy toDummy() {return new Dummy(jwtUpn(token));}
    }
}
