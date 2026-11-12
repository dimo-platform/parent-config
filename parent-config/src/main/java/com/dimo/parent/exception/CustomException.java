package com.dimo.parent.exception;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CustomException extends RuntimeException {

    private final List<Item> items;

    @Builder
    public CustomException(List<Item> items, Throwable cause) {
        super(buildMessage(items), cause);
        this.items = items == null ? List.of() : List.copyOf(items);
    }


    private static String buildMessage(List<Item> items) {

        if (items == null || items.isEmpty()) {
            return "CustomException without error items";
        }

        return items.stream()
                .map(Item::getCode)
                .collect(Collectors.joining(", ", "CustomException: ", ""));

    }


    @Getter
    public static class Item {

        private final String code;

        private final Object[] args;

        @Builder
        private Item(String code, Object[] args) {
            this.code = code;
            this.args = args == null ? new Object[0] : args;
        }

    }
}