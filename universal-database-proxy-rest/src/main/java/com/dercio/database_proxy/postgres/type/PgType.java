package com.dercio.database_proxy.postgres.type;

import com.dercio.database_proxy.openapi.OpenApiType;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
public enum PgType {
    INTEGER("integer", OpenApiType.INTEGER, PgType::blankToNull, Integer::parseInt),
    NUMERIC("numeric", OpenApiType.NUMBER, PgType::blankToNull, BigDecimal::new),
    BIGINT("bigint", OpenApiType.INTEGER, PgType::blankToNull, Long::parseLong),
    BOOLEAN("boolean", OpenApiType.BOOLEAN, PgType::blankToNull, Boolean::parseBoolean),
    DATE("date", OpenApiType.STRING, PgType::blankToNull, Function.identity()),
    CHARACTER_VARYING("character varying", OpenApiType.STRING, PgType::blankToNull, Function.identity()),
    UUID("uuid", OpenApiType.STRING, PgType::blankToNull, Function.identity()),
    TEXT("text", OpenApiType.STRING, PgType::blankToNull, Function.identity()),
    CHARACTER("character", OpenApiType.STRING, PgType::blankToNull, Function.identity()),
    TIMESTAMP_WITHOUT_TIME_ZONE("timestamp without time zone", OpenApiType.STRING, PgType::blankToNull, LocalDateTime::parse),
    TIMESTAMP_WITH_TIME_ZONE("timestamp with time zone", OpenApiType.STRING, PgType::blankToNull, OffsetDateTime::parse),
    JSON("json", OpenApiType.OBJECT, PgType::blankToNull, JsonObject::new),
    JSONB("jsonb", OpenApiType.OBJECT, PgType::blankToNull, JsonObject::new),
    USER_DEFINED("USER-DEFINED", OpenApiType.ANY, PgType::blankToNull, Function.identity()),
    UNKNOWN("UNKNOWN", OpenApiType.ANY, PgType::blankToNull, Function.identity());

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

    public Object toSqlValue(Object value) {
        if (value == null) return null;

        return switch (this) {
            case TIMESTAMP_WITH_TIME_ZONE, TIMESTAMP_WITHOUT_TIME_ZONE -> parse(value.toString());
            case JSON, JSONB -> value instanceof JsonObject jsonObject ? jsonObject.encode() : value.toString();
            default -> value;
        };
    }

    public Object toRowValue(Object value) {
        if (value == null) return null;

        return switch (this) {
            case JSON, JSONB -> value instanceof JsonObject ? value : parse(value.toString());
            default -> value;
        };
    }

    public String placeholder() {
        return switch (this) {
            case JSON -> "?::json";
            case JSONB -> "?::jsonb";
            default -> "?";
        };
    }

    public static Object toSqlValue(String type, Object value) {
        return from(type).toSqlValue(value);
    }

    public static Object toRowValue(String type, Object value) {
        return from(type).toRowValue(value);
    }

    public static String placeholder(String type) {
        return from(type).placeholder();
    }

    public static String fromPgToOpenApiType(String type) {
        return from(type).getOpenApiType();
    }

    public static Object parse(String type, String value) {
        return from(type).parse(value);
    }

    private static String blankToNull(String value) {
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


