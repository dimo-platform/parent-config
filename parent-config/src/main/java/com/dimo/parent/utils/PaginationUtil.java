package com.dimo.parent.utils;

public final class PaginationUtil {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private PaginationUtil() {
    }

    public static int normalizePage(Integer page) {
        return page == null || page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
    }

    public static int normalizeSize(Integer size) {
        return size == null || size < 1 ? DEFAULT_SIZE : size;
    }

    public static int normalizeSize(Integer size, int defaultSize, int maxSize) {
        int normalizedSize = size == null || size < 1 ? defaultSize : size;
        return Math.min(normalizedSize, maxSize);
    }
}