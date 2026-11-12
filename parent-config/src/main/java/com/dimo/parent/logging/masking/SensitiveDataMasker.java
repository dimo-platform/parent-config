package com.dimo.parent.logging.masking;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Redacts secrets and private content from log text. It works on free text and on the JSON text
/// produced by `LoggingUtil`, so it applies to the message, MDC values and stack traces alike.
@UtilityClass
public class SensitiveDataMasker {

    public static final String MASK = "***";

    private static final String SENSITIVE_KEYS = String.join("|",
            "password", "passwd", "newPassword", "oldPassword", "passwordHash", "secret", "clientSecret",
            "otp", "verificationCode", "smsCode", "pin",
            "token", "accessToken", "refreshToken", "resetToken", "idToken", "apiKey", "api-key", "privateKey",
            "authorization", "cookie", "set-cookie",
            "content", "text", "body", "messageText", "plaintext", "ciphertext", "caption");

    /// `"key":"value"`, `"key": value`, `key=value` and `key: value`. Keys are matched as whole words, and
    /// `message`/`code` are deliberately absent: they carry event descriptions and error codes operators need.
    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)((?<![A-Za-z0-9])[\"']?(?:" + SENSITIVE_KEYS + ")[\"']?\\s*[:=]\\s*)(\"(?:[^\"\\\\]|\\\\.)*\"|'[^']*'|[^\\s,;}&\\]\"']+)");

    private static final Pattern BEARER = Pattern.compile("(?i)\\b(Bearer|Basic)\\s+[A-Za-z0-9._~+/=-]{8,}");

    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]*");


    public String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = JWT.matcher(text).replaceAll(MASK);
        masked = BEARER.matcher(masked).replaceAll("$1 " + MASK);
        return maskKeyValues(masked);
    }

    private String maskKeyValues(String text) {
        Matcher matcher = KEY_VALUE.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = matcher.group(2);
            String replacement = value.startsWith("\"") ? "\"" + MASK + "\""
                    : value.startsWith("'") ? "'" + MASK + "'" : MASK;
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
