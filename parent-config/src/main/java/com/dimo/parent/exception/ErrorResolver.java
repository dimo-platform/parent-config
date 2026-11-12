package com.dimo.parent.exception;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;

@Service
public class ErrorResolver {

    /// fixed locales of the localized error details
    public static final Locale ENGLISH_LOCALE = Locale.ENGLISH;
    public static final Locale PERSIAN_LOCALE = Locale.forLanguageTag("fa");

    // prefix of the entity-name argument keys resolved through the bundles
    private static final String ENTITY_ARG_PREFIX = "entity.";


    private final MessageSource errorMessageSource;

    public ErrorResolver(@Qualifier(value = "errorMessageSource") MessageSource errorMessageSource) {
        this.errorMessageSource = errorMessageSource;
    }


    public String resolve(String code, Locale locale, Object... args) {
        return errorMessageSource.getMessage(code, resolveArgs(locale, args), code, locale);
    }

    public String resolve(CustomException.Item item, Locale locale) {
        return resolve(item.getCode(), locale, item.getArgs());
    }


    /**
     * Localizes entity-name arguments and leaves every other argument as is.
     */
    private Object[] resolveArgs(Locale locale, Object... args) {

        if (args == null || args.length == 0) {
            return new Object[0];
        }

        return Arrays.stream(args)
                .map(arg -> arg instanceof String key && key.startsWith(ENTITY_ARG_PREFIX)
                        ? errorMessageSource.getMessage(key, null, key, locale)
                        : arg)
                .toArray();

    }
}
