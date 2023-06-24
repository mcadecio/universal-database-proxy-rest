package com.dercio.database_proxy.common.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String path;
    private String message;
    private int code;
    private List<ErrorField> errors;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ErrorResponse errorResponse;

        private Builder() {
            errorResponse = new ErrorResponse();
            errorResponse.setErrors(new ArrayList<>());
            errorResponse.setTimestamp(LocalDateTime.now());
        }

        public Builder path(String path) {
            errorResponse.setPath(path);
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            errorResponse.setTimestamp(timestamp);
            return this;
        }

        public Builder code(int code) {
            errorResponse.setCode(code);
            return this;
        }

        public Builder message(String message) {
            errorResponse.setMessage(message);
            return this;
        }

        public Builder addErrors(List<ErrorField> errors) {
            errorResponse.getErrors().addAll(errors);
            return this;
        }

        public ErrorResponse build() {
            return errorResponse;
        }
    }
}
