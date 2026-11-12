package com.dimo.parent.response;

import lombok.Builder;

@Builder
public record ValidationErrorDto(

        String errorCode,

        String messageFa,

        String messageEn

    ) {}