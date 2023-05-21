package com.dercio.database_proxy.postgres.type;

import com.dercio.database_proxy.openapi.OpenApiType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
public enum PgType {
    INTEGER("integer", OpenApiType.INTEGER, PgType::blankSanitizer, Integer::parseInt),
    NUMERIC("numeric", OpenApiType.NUMBER, PgType::blankSanitizer, BigDecimal::new),
    BIGINT("bigint", OpenApiType.INTEGER, PgType::blankSanitizer, Long::parseLong),
    BOOLEAN("boolean", OpenApiType.BOOLEAN, PgType::blankSanitizer, Boolean::parseBoolean),
    DATE("date", OpenApiType.STRING, PgType::blankSanitizer, Function.identity()),
    CHARACTER_VARYING("character varying", OpenApiType.STRING, PgType::emptySanitizer, Function.identity()),
    UUID("uuid", OpenApiType.STRING, PgType::blankSanitizer, Function.identity()),
    TEXT("text", OpenApiType.STRING, PgType::emptySanitizer, Function.identity()),
    CHARACTER("character", OpenApiType.STRING, PgType::emptySanitizer, Function.identity()),
    TIMESTAMP_WITHOUT_TIME_ZONE("timestamp without time zone", OpenApiType.STRING, PgType::emptySanitizer, LocalDateTime::parse),
    TIMESTAMP_WITH_TIME_ZONE("timestamp with time zone", OpenApiType.STRING, PgType::emptySanitizer, OffsetDateTime::parse),
    JSON("json", OpenApiType.OBJECT, PgType::blankSanitizer, Function.identity()),
    JSONB("jsonb", OpenApiType.OBJECT, PgType::blankSanitizer, Function.identity()),
    USER_DEFINED("USER-DEFINED", OpenApiType.ANY, PgType::blankSanitizer, Function.identity()),
    UNKNOWN("UNKNOWN", OpenApiType.ANY, PgType::blankSanitizer, Function.identity());

    @Getter
    private final String dbType;

    @Getter
    private final String openApiType;

    private final Function<String, String> sanitizer;
    private final Function<String, ?> mapper;

    public <T> T parse(String value) {
        var sanitizedValue = sanitizer.apply(value);

        return sanitizedValue == null ? null: (T) mapper.apply(sanitizedValue);
    }

    public static String fromPgToOpenApiType(String type) {
        return from(type).getOpenApiType();
    }

    public static Object parse(String type, String value) {
        return from(type).parse(value);
    }

    private static String blankSanitizer(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String emptySanitizer(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static PgType from(String type) {
        var desiredType = type.toUpperCase()
                .replace(" ", "_")
                .replace("-", "_");
        return valueOfOrUnknown(desiredType);
    }

    private static PgType valueOfOrUnknown(String desiredType) {
        return Stream.of(values())
                .filter(pgType -> pgType.toString().equals(desiredType))
                .findFirst()
                .orElse(PgType.UNKNOWN);
    }
}


