package com.spring.springboot.smartlink.utils;

import java.util.regex.Pattern;

import com.spring.springboot.smartlink.advices.exceptions.InvalidLinkHashExceptionSmartLink;

public final class Base62 {

    private static final String DIGITS = "0123456789";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String CHARS = DIGITS + LOWER + UPPER;

    private static final Pattern HASH_PATTERN = Pattern.compile("^[0-9a-zA-Z]+$");

    private Base62() {
    }

    public static String encode(Long num) {
        StringBuilder generatedHash = new StringBuilder();
        while (num > 0) {
            char ch = CHARS.charAt((int) (num % 62));
            generatedHash.append(ch);
            num /= 62;
        }
        return generatedHash.reverse().toString();
    }

    public static Long decode(String hash, String invalidHashMessage) {
        if (hash == null || !HASH_PATTERN.matcher(hash).matches()) {
            throw new InvalidLinkHashExceptionSmartLink(String.format(invalidHashMessage, hash));
        }

        long res = 0L;

        for (int i = 0; i < hash.length(); i++) {
            int ind = CHARS.indexOf(hash.charAt(i));
            if (ind < 0) {
                throw new InvalidLinkHashExceptionSmartLink(String.format(invalidHashMessage, hash));
            }
            res = res * 62 + ind;
        }
        return res;
    }
}
