package com.dimo.parent.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseBody {

    private String message;

    @Builder.Default
    private Object data = Map.of();

    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Long totalElements;

    private List<ValidationErrorDto> errors;
    private List<String> invalidFields;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime timestamp = OffsetDateTime.now();

}
