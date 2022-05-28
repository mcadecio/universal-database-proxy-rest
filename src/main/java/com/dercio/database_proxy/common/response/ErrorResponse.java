package com.dercio.database_proxy.common.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ErrorResponse {
    private final String message;
    private final Integer statusCode;

    public static ErrorResponse of(String message, Integer statusCode) {
        return new ErrorResponse(message, statusCode);
    }

    public String encode() {
        return "{\n" +
                "  \"message\": \"" + message + "\",\n" +
                "  \"statusCode\": " + statusCode + "\n" +
                "}";
    }
}