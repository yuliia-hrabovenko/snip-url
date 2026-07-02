package com.shortener.urlservice.utils;


public final class Base62 {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final char[] LOOKUP_MAP = ALPHABET.toCharArray();

    private Base62() {}

    public static String encode(long number) {
        if (number == 0) {
            return String.valueOf(LOOKUP_MAP[0]);
        }

        StringBuilder builder = new StringBuilder();
        long temp = number;
        while (temp > 0) {
            int remainder = (int) (temp % BASE);
            builder.append(LOOKUP_MAP[remainder]);
            temp /= BASE;
        }
        return builder.reverse().toString();
    }

    public static long decode(String base62Str) {
        if (base62Str == null || base62Str.isBlank()) {
            throw new IllegalArgumentException("Target code string cannot be null or empty");
        }

        long decodedValue = 0;
        for (int i = 0; i < base62Str.length(); i++) {
            char character = base62Str.charAt(i);
            int index = ALPHABET.indexOf(character);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base62 character encountered: " + character);
            }
            decodedValue = decodedValue * BASE + index;
        }
        return decodedValue;
    }
}
