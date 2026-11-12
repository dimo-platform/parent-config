package com.dimo.parent.logging.masking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonWriter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataJsonCustomizerTest {

    private final SensitiveDataJsonCustomizer customizer = new SensitiveDataJsonCustomizer();


    @Test
    void customize_masksMessageMdcAndStackTraceMembersOfAJsonLogEvent() {
        JsonWriter<Map<String, String>> writer = JsonWriter.<Map<String, String>>of(members -> {
            members.add("message", event -> event.get("message"));
            members.add("traceId", event -> event.get("traceId"));
            members.add("stack_trace", event -> event.get("stack_trace"));
            customizer.customize((JsonWriter.Members) members);
        });
        Map<String, String> event = Map.of(
                "message", "sms sent otp=654321 content=\"private text\"",
                "traceId", "4bf92f3577b34da6a3ce929d0e0e4736",
                "stack_trace", "IllegalStateException: password=hunter2");

        String json = writer.writeToString(event);

        assertThat(json)
                .doesNotContain("654321")
                .doesNotContain("private text")
                .doesNotContain("hunter2")
                .contains("4bf92f3577b34da6a3ce929d0e0e4736");
    }
}
