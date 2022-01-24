package com.github.t1.wunderbar.junit.http;

import lombok.AllArgsConstructor;
import lombok.Value;

import static com.github.t1.wunderbar.junit.http.HttpUtils.base64;
import static com.github.t1.wunderbar.junit.http.HttpUtils.base64decode;
import static java.util.Locale.ROOT;

public interface Authorization {
    static Authorization valueOf(String string) {
        if (string == null) return null;
        var split = string.split(" ", 2);
        if (split.length != 2) throw new IllegalArgumentException("unsupported authorization format: need a type");
        var type = split[0].toLowerCase(ROOT);
        var credentials = split[1];
        switch (type) {
            case "dummy":
                return new Dummy(credentials);
            case "basic":
                return new Basic(credentials);
            case "bearer":
                return new Bearer(credentials);
            default:
                throw new IllegalArgumentException("unsupported authorization type " + type);
        }
    }

    /** The credentials as used for the http <code>Authorization</code> header */
    String toHeader();

    @Value class Dummy implements Authorization {
        public static final Dummy INSTANCE = new Dummy("authorization");

        private Dummy(String credentials) {assert "authorization" .equals(credentials);}

        @Override public String toString() {return "Dummy authorization";}

        @Override public String toHeader() {throw new UnsupportedOperationException();}
    }

    @AllArgsConstructor
    @Value class Basic implements Authorization {
        String username;
        String password;

        public Basic(String credentials) {
            var split = base64decode(credentials).split(":", 2);
            if (split.length != 2) throw new IllegalArgumentException("invalid basic authorization format");
            this.username = split[0];
            this.password = split[1];
        }

        @Override public String toString() {return "Basic " + base64(":");}

        @Override public String toHeader() {return "Basic " + base64(username + ":" + password);}
    }

    @Value class Bearer implements Authorization {
        String token;

        @Override public String toString() {return "Bearer token";}

        @Override public String toHeader() {return "Bearer " + token;}
    }
}
