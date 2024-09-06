package test.tools;

import io.smallrye.jwt.build.Jwt;

import java.time.Duration;

public class GenerateToken {
    public static void main(String[] args) {
        System.setProperty("smallrye.jwt.sign.key.location", "privateKey.pem");
        String token =
                Jwt.issuer("https://github.com/t1")
                        .upn("jane@doe.com")
                        .groups("Writer")
                        .expiresIn(Duration.ofDays(100 * 356))
                        .sign();
        System.out.println(token);
    }
}
