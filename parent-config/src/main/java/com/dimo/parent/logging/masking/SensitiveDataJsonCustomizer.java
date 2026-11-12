package com.dimo.parent.logging.masking;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/// Masks every string member of a structured (JSON) log event, including MDC values and stack traces.
/// Registered through `logging.structured.json.customizer` by the shared observability defaults.
public class SensitiveDataJsonCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

    @Override
    public void customize(JsonWriter.Members<Object> members) {
        members.applyingValueProcessor(JsonWriter.ValueProcessor.of(String.class, SensitiveDataMasker::mask));
    }
}
