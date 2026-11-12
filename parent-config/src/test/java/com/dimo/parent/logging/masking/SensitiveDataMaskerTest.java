package com.dimo.parent.logging.masking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void mask_hidesPasswordAndOtpInJsonText() {
        String input = "{\"message\":\"Executing method\",\"data\":[{\"username\":\"ali\",\"password\":\"S3cret!pw\","
                + "\"otp\":\"123456\"}]}";

        String masked = SensitiveDataMasker.mask(input);

        assertThat(masked)
                .doesNotContain("S3cret!pw")
                .doesNotContain("123456")
                .contains("\"username\":\"ali\"")
                .contains("\"message\":\"Executing method\"");
    }

    @Test
    void mask_hidesChatMessageContent() {
        String masked = SensitiveDataMasker.mask("{\"clientMessageId\":\"c1\",\"content\":\"hello secret chat\",\"type\":\"TEXT\"}");

        assertThat(masked)
                .doesNotContain("hello secret chat")
                .contains("\"type\":\"TEXT\"");
    }

    @Test
    void mask_hidesTokensAndAuthorizationHeaders() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjMifQ.c2lnbmF0dXJl";

        String masked = SensitiveDataMasker.mask("Authorization: Bearer " + jwt + " refreshToken=abc123xyz accessToken=\"" + jwt + "\"");

        assertThat(masked)
                .doesNotContain(jwt)
                .doesNotContain("abc123xyz");
    }

    @Test
    void mask_hidesKeyValuePairsInFreeText() {
        String masked = SensitiveDataMasker.mask("login failed password=hunter2 verificationCode: 998877 for user=ali");

        assertThat(masked)
                .doesNotContain("hunter2")
                .doesNotContain("998877")
                .contains("user=ali");
    }

    @Test
    void mask_keepsErrorCodesAndUnrelatedWordsContainingSensitiveKeys() {
        String masked = SensitiveDataMasker.mask("{\"code\":\"error.entity.not.found\",\"context\":\"orders\"}");

        assertThat(masked).isEqualTo("{\"code\":\"error.entity.not.found\",\"context\":\"orders\"}");
    }

    @Test
    void mask_returnsNullAndEmptyUnchanged() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
        assertThat(SensitiveDataMasker.mask("")).isEmpty();
    }
}
