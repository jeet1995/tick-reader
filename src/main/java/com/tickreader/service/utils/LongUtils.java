package com.tickreader.service.utils;

public class LongUtils {

    private LongUtils() {}

    public static int safeCompare(Long a, Long b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return Long.compare(a, b);
    }
}
