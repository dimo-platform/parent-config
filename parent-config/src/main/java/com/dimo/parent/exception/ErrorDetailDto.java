package com.dimo.parent.exception;

/**
 * Localized error detail of a failed response.
 *
 * @param errorCode stable error code resolvable through the error resource bundles
 * @param messageEn the English message of the error
 * @param messageFa the Persian message of the error
 */
public record ErrorDetailDto(
        String errorCode,
        String messageEn,
        String messageFa) {
}