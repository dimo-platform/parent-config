package com.dimo.parent.utils;

import lombok.experimental.UtilityClass;
import com.dimo.parent.exception.CustomException;

import java.util.List;

@UtilityClass
public class CustomExceptionUtil {

    /// Single Item exception

    public CustomException buildSingleItemException(CustomException.Item item) {
        return new CustomException(List.of(item), null);
    }

    public CustomException buildSingleItemException(String errorCode, String... args) {
        List<CustomException.Item> items = List.of(CustomException.Item.builder()
                .code(errorCode)
                .args(args)
                .build());

        return new CustomException(items, null);
    }

    /// Multiple items exception

    public CustomException buildCustomException(List<CustomException.Item> items) {
        return new CustomException(items, null);
    }


    // reusable error codes of the common entity errors
    private static final String ENTITY_NOT_FOUND_CODE = "error.entity.not.found";
    private static final String ENTITY_DUPLICATE_CODE = "error.entity.duplicate";



    /// Single error factories

    public static CustomException withError(String errorCode, String... args) {
        return CustomException.builder()
                .items(List.of(item(errorCode, args)))
                .build();
    }

    public static CustomException withError(String errorCode, Throwable cause, String... args) {
        return CustomException.builder()
                .items(List.of(item(errorCode, args)))
                .cause(cause)
                .build();
    }

    public static CustomException ofItems(List<CustomException.Item> items) {
        return CustomException.builder()
                .items(items)
                .build();
    }


    /// Common entity error factories

    public static CustomException entityNotFound(String entityKey, Object identifier) {
        return withError(ENTITY_NOT_FOUND_CODE, entityKey, String.valueOf(identifier));
    }

    public static CustomException duplicateEntity(String entityKey, Object identifier) {
        return withError(ENTITY_DUPLICATE_CODE, entityKey, String.valueOf(identifier));
    }


    /// Item factory

    public static CustomException.Item item(String errorCode, String... args) {
        return CustomException.Item.builder()
                .code(errorCode)
                .args(args)
                .build();
    }
}
