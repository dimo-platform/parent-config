package com.dimo.parent.security.authentication.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import com.dimo.parent.exception.ErrorResolver;
import com.dimo.parent.response.ResponseBody;
import com.dimo.parent.response.RestResponseHandler;
import com.dimo.parent.response.ValidationErrorDto;
import com.dimo.parent.utils.CustomExceptionUtil;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes security failures as JSON through the standard {@link com.dimo.parent.response.ResponseBody}
 * envelope of the REST layer. Only stable error codes and their localized
 * messages are exposed; never stack traces or credential data.
 */
@Component
@RequiredArgsConstructor
public class SecurityResponseWriter {

    // handlers
    private final ErrorResolver errorResolver;
    private final ObjectMapper objectMapper;
    private final RestResponseHandler responseHandler;

    /**
     * Serializes a failed envelope for the given status and error code.
     */
    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      HttpStatus status,
                      String errorCode,
                      String... args) throws IOException {

        var item = CustomExceptionUtil.item(errorCode, args);

        List<ValidationErrorDto> errors = List.of(new ValidationErrorDto(
                errorCode,
                errorResolver.resolve(item, ErrorResolver.PERSIAN_LOCALE),
                errorResolver.resolve(item, ErrorResolver.ENGLISH_LOCALE)));

        String message = errorResolver.resolve(item, LocaleContextHolder.getLocale());
        ResponseBody body = responseHandler.handle(status, errors, message).getBody();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getOutputStream(), body);

    }

}
