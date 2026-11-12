package com.dimo.parent.logging.definition;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class LogEvent {

    private String timestamp;

    private String level;

    private String service;

    private String className;

    private String method;

    private String thread;

    private String traceId;

    private String requestId;

    private String userId;

    private String message;

    @Builder.Default
    private Object data = Map.of();

    private ErrorData error;
}