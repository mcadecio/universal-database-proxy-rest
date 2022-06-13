package com.dercio.database_proxy.common.error;

import lombok.*;

import java.util.Objects;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorField {
    private final String fieldName;
    private final String message;

    public static ErrorField of(@NonNull String fieldName, @NonNull String message) {
        Objects.requireNonNull(fieldName, "The fieldName should not be null");
        Objects.requireNonNull(message, "The message should not be null");

        return new ErrorField(fieldName, message);
    }

}
