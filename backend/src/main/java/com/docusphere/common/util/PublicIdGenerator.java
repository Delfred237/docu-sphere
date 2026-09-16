package com.docusphere.common.util;

import java.security.SecureRandom;

public class PublicIdGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicIdGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
