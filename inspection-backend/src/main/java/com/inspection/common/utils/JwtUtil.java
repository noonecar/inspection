package com.inspection.common.utils;

import com.inspection.common.constant.AuthConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private final Key key;
    private final long expireMs;

    public JwtUtil(String secret, long expireMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMs = expireMs;
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMs);
        return Jwts.builder()
                .setSubject(username)
                .claim(AuthConstants.CLAIM_USERNAME, username)
                .claim(AuthConstants.CLAIM_ROLE, role)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
