package com.dimo.parent.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.dimo.parent.response.ResponseBody;
import com.dimo.parent.response.RestResponseHandler;
import com.dimo.parent.response.ValidationErrorDto;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    // default properties
    private static final Locale EN = Locale.ENGLISH;
    private static final Locale FA = Locale.forLanguageTag("fa");

    // resolver
    private final ErrorResolver errorResolver;
    private final RestResponseHandler responseHandler;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ResponseBody> handleCustomException(CustomException ex) {

        return responseHandler.handle(
                HttpStatus.BAD_REQUEST, resolveValidationErrors(ex), "Custom Exception");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseBody> handleAccessDenied(AccessDeniedException ex) {

        return responseHandler.handle(
                HttpStatus.FORBIDDEN,
                List.of(resolveValidationError("error.security.forbidden")),
                "Access Denied");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseBody> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex) {

        return responseHandler.handle(
                HttpStatus.METHOD_NOT_ALLOWED,
                List.of(resolveValidationError("error.method.not.allowed")),
                "Method Not Allowed");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseBody> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, Object> validationErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .filter(fe -> fe.getDefaultMessage() != null)
                        .collect(
                                Collectors.toMap(
                                        FieldError::getField,
                                        DefaultMessageSourceResolvable::getDefaultMessage,
                                        (first, second) -> first));

        return responseHandler.handle(
                HttpStatus.BAD_REQUEST,
                resolveValidationErrors("error.method.argument.not.allowed", validationErrors),
                "Validation Failed");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseBody> handleConstraintViolation(ConstraintViolationException ex) {

        Map<String, Object> validationErrors =
                ex.getConstraintViolations().stream()
                        .collect(
                                Collectors.toMap(
                                        item -> item.getPropertyPath().toString(),
                                        ConstraintViolation::getMessage,
                                        (first, second) -> first));

        return responseHandler.handle(
                HttpStatus.BAD_REQUEST,
                resolveValidationErrors("error.field.has.validation.constraint", validationErrors),
                "Constraint Violation");
    }

    @ExceptionHandler(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseBody> handleInvalidParameter(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception) {
        return responseHandler.handle(
                HttpStatus.BAD_REQUEST, resolveValidationErrors("error.validation"), "Bad Request");
    }

    @ExceptionHandler({
            org.springframework.web.bind.ServletRequestBindingException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class
    })
    public ResponseEntity<ResponseBody> handleInvalidRequest(Exception exception) {
        return responseHandler.handle(
                HttpStatus.BAD_REQUEST, resolveValidationErrors("error.validation"), "Bad Request");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseBody> handleMissingResource(NoResourceFoundException exception) {
        return responseHandler.handle(
                HttpStatus.NOT_FOUND,
                resolveValidationErrors("error.resource.not.found"),
                "Not Found");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ResponseBody> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String code =
                switch (status) {
                    case NOT_FOUND -> "error.resource.not.found";
                    case CONFLICT -> "error.concurrent.modification";
                    case FORBIDDEN -> "error.security.forbidden";
                    case BAD_REQUEST -> "error.validation";
                    default -> "error.internal.server.error";
                };
        return responseHandler.handle(
                status, resolveValidationErrors(code), status.getReasonPhrase());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ResponseBody> handleConcurrentModification(
            OptimisticLockingFailureException exception) {
        return responseHandler.handle(
                HttpStatus.CONFLICT,
                resolveValidationErrors("error.concurrent.modification"),
                "Conflict");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseBody> handleUnhandled(Exception ex) {
        log.error("Unhandled REST request failure", ex);

        return responseHandler.handle(
                HttpStatus.INTERNAL_SERVER_ERROR,
                resolveValidationErrors("error.internal.server.error"),
                "Internal Server Error");
    }

    private List<ValidationErrorDto> resolveValidationErrors(
            String errorCode, Map<String, Object> validationErrors) {
        return validationErrors.entrySet().stream()
                .map(
                        entry -> {
                            String messageFa =
                                    errorResolver.resolve(
                                            errorCode, FA, entry.getKey(), entry.getValue());
                            String messageEn =
                                    errorResolver.resolve(
                                            errorCode, EN, entry.getKey(), entry.getValue());

                            return ValidationErrorDto.builder()
                                    .messageFa(messageFa)
                                    .messageEn(messageEn)
                                    .errorCode(errorCode)
                                    .build();
                        })
                .toList();
    }

    private List<ValidationErrorDto> resolveValidationErrors(CustomException ex) {

        return ex.getItems().stream()
                .map(
                        item -> {
                            String errorCode = item.getCode();
                            String messageFa = errorResolver.resolve(item, FA);
                            String messageEn = errorResolver.resolve(item, EN);

                            return ValidationErrorDto.builder()
                                    .errorCode(errorCode)
                                    .messageFa(messageFa)
                                    .messageEn(messageEn)
                                    .build();
                        })
                .toList();
    }

    private List<ValidationErrorDto> resolveValidationErrors(String errorCode) {
        String messageFa = errorResolver.resolve(errorCode, FA);
        String messageEn = errorResolver.resolve(errorCode, EN);

        return List.of(
                ValidationErrorDto.builder()
                        .errorCode(errorCode)
                        .messageFa(messageFa)
                        .messageEn(messageEn)
                        .build());
    }

    private ValidationErrorDto resolveValidationError(String errorCode) {
        String messageFa = errorResolver.resolve(errorCode, FA);
        String messageEn = errorResolver.resolve(errorCode, EN);

        return ValidationErrorDto.builder()
                .messageEn(messageEn)
                .messageFa(messageFa)
                .errorCode(errorCode)
                .build();
    }
}
