package com.dimo.parent.response;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestResponseHandler {

    public ResponseEntity<ResponseBody> handle(HttpStatus status, Object data) {
        return handle(status, data, null);
    }

    // For lists paged without a total-count query.
    public ResponseEntity<ResponseBody> handle(HttpStatus status, Object data, int page, int size) {
        ResponseEntity<ResponseBody> response = handle(status, data);
        if (response.getBody() != null) {
            response.getBody().setPage(page);
            response.getBody().setSize(size);
        }
        return response;
    }

    public ResponseEntity<ResponseBody> handle(HttpStatus status, List<ValidationErrorDto> errors, String message) {
        if (status == HttpStatus.NO_CONTENT) {
            return ResponseEntity.noContent().build();
        }

        ResponseBody.ResponseBodyBuilder responseBodyBuilder = ResponseBody.builder();

        responseBodyBuilder.errors(errors);

        if (!status.is2xxSuccessful()) {
            handlerMessage(responseBodyBuilder, message);
        }

        return ResponseEntity
                .status(status)
                .body(responseBodyBuilder.build());
    }

    public ResponseEntity<ResponseBody> handle(HttpStatus status, Object data, String message) {
        if (status == HttpStatus.NO_CONTENT) {
            return ResponseEntity.noContent().build();
        }

        // a fresh builder per response keeps concurrent requests isolated
        ResponseBody.ResponseBodyBuilder responseBodyBuilder = ResponseBody.builder();

        // process on message
        if (!status.is2xxSuccessful()) {
            handlerMessage(responseBodyBuilder, message);
        }
        // process base on data type .
        dataFactory(responseBodyBuilder, data);

        return ResponseEntity
                .status(status)
                .body(responseBodyBuilder.build());
    }

    private void dataFactory(ResponseBody.ResponseBodyBuilder responseBodyBuilder, Object data) {

        if (data == null) {
            return;
        }

        if (data instanceof Page<?> page) {
            responseBodyBuilder
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .data(page.getContent());
        }
        else {
            responseBodyBuilder
                    .data(data);
        }

    }

    private void handlerMessage(ResponseBody.ResponseBodyBuilder responseBodyBuilder, String message) {

        if (message != null && !message.isEmpty()) {
            responseBodyBuilder.message(message);
        }

    }
}
