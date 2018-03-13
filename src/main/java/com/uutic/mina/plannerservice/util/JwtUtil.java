package com.uutic.mina.plannerservice.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final String SECRET = "uutic.jwt.secret";
    private static final String ISSUER = "uutic";
    private static final Integer EXPIRES_MINUTE = 10;

    public static String encode(Map<String, String> claims) throws UnsupportedEncodingException, JWTCreationException {
        Calendar issueAt = Calendar.getInstance();
        Calendar expireAt = Calendar.getInstance();
        expireAt.add(Calendar.MINUTE, EXPIRES_MINUTE);
        Algorithm algorithm = Algorithm.HMAC256(Base64.encodeBase64(SECRET.getBytes()));
        JWTCreator.Builder builder = JWT.create()
                .withIssuer(ISSUER);
//                .withIssuedAt(issueAt.getTime())
//                .withExpiresAt(expireAt.getTime());
        claims.forEach(builder::withClaim);
        return builder.sign(algorithm);
    }

    public static Map<String, String> decode(String token) throws UnsupportedEncodingException {
        Algorithm algorithm = Algorithm.HMAC256(Base64.encodeBase64(SECRET.getBytes()));
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        DecodedJWT jwt = verifier.verify(token);
        Map<String, String> claims = new HashMap<>();
        jwt.getClaims().forEach((k, v) -> claims.put(k, v.asString()));
        return claims;
    }
}
