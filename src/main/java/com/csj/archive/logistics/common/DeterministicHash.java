package com.csj.archive.logistics.common;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class DeterministicHash {

    public String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public String shortHash(String value) {
        return sha256Hex(value).substring(0, 12).toUpperCase();
    }

    public int positiveInt(String value) {
        String hex = sha256Hex(value).substring(0, 8);
        return (int) (Long.parseLong(hex, 16) & 0x7fffffff);
    }

    public int bounded(String value, int boundExclusive) {
        if (boundExclusive <= 0) {
            throw new IllegalArgumentException("boundExclusive must be positive");
        }
        return positiveInt(value) % boundExclusive;
    }

    public double zeroToOne(String value) {
        return bounded(value, 10_000) / 10_000.0;
    }
}
