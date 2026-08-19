package com.spring.springboot.UrlShortener.services;


public class Base62 {

    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Base62() {
    }

    public static String encode(Long num) {
        if (0 == num) return "0";

        StringBuilder generatedHash = new StringBuilder();
        while (num > 0) {
            char ch = CHARS.charAt((int) (num % 62));
            generatedHash.append(ch);
            num /= 62;
        }
        return generatedHash.reverse().toString();
    }

    public static Long decode(String hash) {
        long res = 0L;

        for (int i = 0; i < hash.length(); i++) {
            int ind = CHARS.indexOf(hash.charAt(i));
            res = res * 62 + ind;
        }
        return res;
    }
}
