package com.fintrex.analytics.auth;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtUtil {

    @Value("${jwt.local-public-pem-path:classpath:/keys/public.pem}")
    private String localPemPath;

    @Value("${jwt.allowed-clock-skew-seconds:180}")
    private long allowedSkew;

    @Value("${jwt.require-issuer:}") // optional; leave empty to not enforce
    private String requireIssuer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile PublicKey publicKey; // lazy-load once

    /**
     * Verify RS256 signature using the bundled public.pem and return parsed
     * wast 
     * claims
     */
    public Jws<Claims> parse(String token) throws JwtException {
        ensurePublicKeyLoaded();

        // Quick sanity: a signed JWT (JWS) has 3 parts; encrypted (JWE) has 5.
        int parts = token == null ? 0 : token.split("\\.").length;
        if (parts != 3) {
            throw new JwtException("Unsupported JWT format: expected 3 parts (JWS), got " + parts);
        }

        JwtParserBuilder builder = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .setAllowedClockSkewSeconds(allowedSkew);

        if (notBlank(requireIssuer)) {
            builder.requireIssuer(requireIssuer);
        }

        return builder.build().parseClaimsJws(token);
    }

    /**
     * Best-effort email extraction
     */
    public String extractEmail(Claims claims) {
        String v;
        if (notBlank(v = claims.get("email", String.class))) {
            return v;
        }
        if (notBlank(v = claims.get("upn", String.class))) {
            return v;
        }
        if (notBlank(v = claims.get("preferred_username", String.class))) {
            return v;
        }
        if (notBlank(v = claims.get("unique_name", String.class))) {
            return v;
        }
        return null;
    }

    /**
     * Human-friendly display name / username (not necessarily your DB login)
     */
    public String extractUsername(Claims claims) {
        String v;
        if (notBlank(v = claims.get("preferred_username", String.class))) {
            return v;
        }
        if (notBlank(v = claims.get("name", String.class))) {
            return v;
        }
        if (notBlank(v = claims.get("given_name", String.class))) {
            return v;
        }
        if (notBlank(v = claims.get("nickname", String.class))) {
            return v;
        }
        v = claims.getSubject();
        return blank(v) ? null : v;
    }

    /**
     * Derive short login (e.g., "janudav") from claims (email/upn/etc.)
     */
    public String extractLogin(Claims claims) {
        String v = firstNonBlank(
                claims.get("login", String.class), // if your Node token adds this
                claims.get("preferred_username", String.class),
                claims.get("upn", String.class),
                claims.get("email", String.class),
                claims.get("name", String.class),
                claims.getSubject()
        );
        if (blank(v)) {
            return null;
        }
        int at = v.indexOf('@');
        if (at > 0) {
            v = v.substring(0, at);
        }
        return v.trim().toLowerCase();
    }

    // ---- internals ----
    private void ensurePublicKeyLoaded() {
        if (publicKey != null) {
            return;
        }
        synchronized (this) {
            if (publicKey != null) {
                return;
            }
            try (InputStream is = openResource(localPemPath)) {
                if (is == null) {
                    throw new IllegalStateException("public.pem not found at " + localPemPath);
                }
                String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                this.publicKey = parseRsaPublicFromPem(pem);
                System.out.println("[JWT] Loaded public key from " + localPemPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load PEM public key from " + localPemPath, e);
            }
        }
    }

    /**
     * Expect SPKI form: -----BEGIN PUBLIC KEY-----
     */
    private static PublicKey parseRsaPublicFromPem(String pem) throws Exception {
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private InputStream openResource(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            String p = path.substring("classpath:".length());
            if (!p.startsWith("/")) {
                p = "/" + p;
            }
            return getClass().getResourceAsStream(p);
        }
        return new FileInputStream(path);
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean notBlank(String s) {
        return !blank(s);
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
