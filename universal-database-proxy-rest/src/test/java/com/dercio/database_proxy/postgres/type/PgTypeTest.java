package com.dercio.database_proxy.postgres.type;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PgTypeTest {

    @ParameterizedTest
    @CsvSource({
            "integer,integer",
            "INTEGER,integer",
            "character varying,string",
            "timestamp with time zone,string",
            "USER-DEFINED,ANY",
            "some random type,ANY"
    })
    void shouldNormalizeDbTypeNamesWhenResolvingOpenApiType(String dbType, String openApiType) {
        assertEquals(openApiType, PgType.fromPgToOpenApiType(dbType));
    }

    @Test
    void shouldParseIntegerValue() {
        assertEquals(42, PgType.parse("integer", "42"));
    }

    @Test
    void shouldParseNumericValueAsBigDecimal() {
        assertEquals(new BigDecimal("12.34"), PgType.parse("numeric", "12.34"));
    }

    @Test
    void shouldParseBigIntValueAsLong() {
        assertEquals(9001L, PgType.parse("bigint", "9001"));
    }

    @Test
    void shouldParseBooleanValue() {
        assertEquals(true, PgType.parse("boolean", "true"));
    }

    @Test
    void shouldReturnStringValuesUnchangedForTextTypes() {
        assertEquals("hello", PgType.parse("text", "hello"));
        assertEquals("hello", PgType.parse("character varying", "hello"));
        assertEquals("2f1c", PgType.parse("uuid", "2f1c"));
    }

    @Test
    void shouldParseJsonValueIntoJsonObject() {
        Object parsed = PgType.parse("jsonb", "{\"a\":1}");

        assertInstanceOf(JsonObject.class, parsed);
        assertEquals(1, ((JsonObject) parsed).getInteger("a"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"integer", "numeric", "bigint", "boolean", "uuid", "jsonb"})
    void shouldReturnNullForBlankInput(String type) {
        assertNull(PgType.parse(type, "   "));
        assertNull(PgType.parse(type, null));
    }

    @ParameterizedTest
    @CsvSource({
            "json,?::json",
            "jsonb,?::jsonb",
            "integer,?",
            "text,?",
            "character varying,?"
    })
    void shouldGenerateTypeAwarePlaceholders(String type, String placeholder) {
        assertEquals(placeholder, PgType.placeholder(type));
    }

    @Test
    void shouldEncodeJsonObjectToStringForSqlValue() {
        JsonObject value = new JsonObject().put("a", 1);

        assertEquals("{\"a\":1}", PgType.toSqlValue("jsonb", value));
    }

    @Test
    void shouldParseTimestampStringWhenConvertingToSqlValue() {
        Object sqlValue = PgType.toSqlValue("timestamp without time zone", "2023-06-24T10:12:28");

        assertEquals(LocalDateTime.parse("2023-06-24T10:12:28"), sqlValue);
    }

    @Test
    void shouldParseOffsetTimestampStringWhenConvertingToSqlValue() {
        Object sqlValue = PgType.toSqlValue("timestamp with time zone", "2023-06-24T10:12:28+01:00");

        assertEquals(OffsetDateTime.parse("2023-06-24T10:12:28+01:00"), sqlValue);
    }

    @Test
    void shouldKeepNonJsonSqlValuesUnchanged() {
        assertEquals(42, PgType.toSqlValue("integer", 42));
        assertNull(PgType.toSqlValue("integer", null));
    }

    @Test
    void shouldParseJsonStringWhenConvertingToRowValue() {
        Object rowValue = PgType.toRowValue("jsonb", "{\"a\":1}");

        assertInstanceOf(JsonObject.class, rowValue);
        assertEquals(1, ((JsonObject) rowValue).getInteger("a"));
    }

    @Test
    void shouldKeepJsonObjectUnchangedWhenConvertingToRowValue() {
        JsonObject value = new JsonObject().put("a", 1);

        assertTrue(value == PgType.toRowValue("jsonb", value));
    }
}
