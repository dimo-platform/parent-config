package com.dimo.parent.logging.definition;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class ErrorData {

    private String exceptionType;

    @Builder.Default
    private Object errors = Map.of();
}