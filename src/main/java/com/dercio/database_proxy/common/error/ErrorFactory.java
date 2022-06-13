package com.dercio.database_proxy.common.error;

import java.util.Collections;
import java.util.List;


public class ErrorFactory {
    public ErrorResponse createErrorResponse(int code, String requestURI, Exception e) {
        return createErrorResponse(code, requestURI, e.getMessage());
    }

    public ErrorResponse createErrorResponse(int code, String requestURI, String message, List<ErrorField> errors) {
        return ErrorResponse.builder()
                            .code(code)
                            .path(requestURI)
                            .message(message)
                            .addErrors(errors)
                            .build();
    }

    public ErrorResponse createErrorResponse(int code, String requestURI, String message) {
        return createErrorResponse(code, requestURI, message, Collections.emptyList());
    }
}
