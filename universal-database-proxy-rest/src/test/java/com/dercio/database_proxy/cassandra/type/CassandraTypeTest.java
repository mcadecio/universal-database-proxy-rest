package com.dercio.database_proxy.cassandra.type;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CassandraTypeTest {

    @ParameterizedTest
    @CsvSource({
            "ascii,string",
            "text,string",
            "varchar,string",
            "uuid,string",
            "timeuuid,string",
            "timestamp,string",
            "date,string",
            "time,string",
            "inet,string",
            "blob,string",
            "int,integer",
            "bigint,integer",
            "smallint,integer",
            "tinyint,integer",
            "varint,integer",
            "counter,integer",
            "float,number",
            "double,number",
            "decimal,number",
            "boolean,boolean"
    })
    void shouldMapCqlTypesToOpenApiTypes(String cqlType, String expectedOpenApiType) {
        assertEquals(expectedOpenApiType, CassandraType.toOpenApiType(cqlType));
    }

    @ParameterizedTest
    @ValueSource(strings = {"list<text>", "map<text, text>", "frozen<address>", "duration"})
    void shouldFallBackToAnyForUnsupportedCollectionsAndUserDefinedTypes(String cqlType) {
        assertEquals("ANY", CassandraType.toOpenApiType(cqlType));
    }

    @ParameterizedTest
    @CsvSource({
            "set<text>,string",
            "set<int>,integer",
            "set<bigint>,integer",
            "set<double>,number",
            "set<boolean>,boolean",
            "set<uuid>,string",
            "set<timestamp>,string",
            "frozen<set<text>>,string",
            "SET<TEXT>,string"
    })
    void shouldMapSetColumnsToAnArrayOfTheElementType(String cqlType, String expectedItemsType) {
        assertAll(
                () -> assertEquals("array", CassandraType.toOpenApiType(cqlType)),
                () -> assertEquals(expectedItemsType, CassandraType.toOpenApiItemsType(cqlType)),
                () -> assertTrue(CassandraType.isSet(cqlType))
        );
    }

    @Test
    void shouldNotTreatScalarsOrOtherCollectionsAsSets() {
        assertAll(
                () -> assertFalse(CassandraType.isSet("text")),
                () -> assertFalse(CassandraType.isSet("list<text>")),
                () -> assertFalse(CassandraType.isSet("map<text, int>")),
                () -> assertFalse(CassandraType.isSet(null)),
                () -> assertNull(CassandraType.toOpenApiItemsType("text"))
        );
    }

    @Test
    void shouldSurfaceASetOfAnUnsupportedElementTypeAsAnArrayOfAny() {
        assertAll(
                () -> assertEquals("array", CassandraType.toOpenApiType("set<frozen<address>>")),
                () -> assertEquals("ANY", CassandraType.toOpenApiItemsType("set<frozen<address>>"))
        );
    }

    @Test
    void shouldBindSetColumnsAsAJavaSetOfDriverTypedElements() {
        // The driver has no codec for a List where a set<int> is expected, nor for a String element
        // where an int is expected, so both the collection type and the element type have to be right.
        var value = CassandraType.toSqlValue("set<int>", new JsonArray().add(3).add(5));

        assertAll(
                () -> assertInstanceOf(Set.class, value),
                () -> assertEquals(Set.of(3, 5), value),
                () -> assertTrue(((Set<?>) value).stream().allMatch(Integer.class::isInstance))
        );
    }

    @Test
    void shouldBindSetElementsThatNeedConversionFromStrings() {
        var uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var value = CassandraType.toSqlValue("set<uuid>", new JsonArray().add(uuid.toString()));

        assertEquals(Set.of(uuid), value);
    }

    @Test
    void shouldDeduplicateAndPreserveOrderWhenBindingASet() {
        var value = CassandraType.toSqlValue("set<text>", new JsonArray().add("jazz").add("cool").add("jazz"));

        assertAll(
                () -> assertEquals(2, ((Set<?>) value).size()),
                () -> assertEquals(List.of("jazz", "cool"), List.copyOf((Set<?>) value))
        );
    }

    @Test
    void shouldRenderASetReadBackFromTheDriverAsAJsonArray() {
        var uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertAll(
                () -> assertEquals(new JsonArray().add("jazz"),
                        CassandraType.toRowValue("set<text>", new LinkedHashSet<>(List.of("jazz")))),
                () -> assertEquals(new JsonArray().add(3).add(5),
                        CassandraType.toRowValue("set<int>", new LinkedHashSet<>(List.of(3, 5)))),
                // Elements with no JSON representation are rendered the same way scalars are.
                () -> assertEquals(new JsonArray().add(uuid.toString()),
                        CassandraType.toRowValue("set<uuid>", new LinkedHashSet<>(List.of(uuid))))
        );
    }

    @Test
    void shouldTreatNullAndEmptySetsConsistently() {
        assertAll(
                () -> assertNull(CassandraType.toSqlValue("set<text>", null)),
                () -> assertNull(CassandraType.toRowValue("set<text>", null)),
                () -> assertEquals(Set.of(), CassandraType.toSqlValue("set<text>", new JsonArray())),
                () -> assertEquals(new JsonArray(), CassandraType.toRowValue("set<text>", Set.of()))
        );
    }

    @Test
    void shouldParseASetFilterValueAsASingleElement() {
        // Set columns are filtered with CONTAINS, which binds one element rather than a whole set.
        assertAll(
                () -> assertEquals("jazz", CassandraType.parse("set<text>", "jazz")),
                () -> assertEquals(7, CassandraType.parse("set<int>", "7")),
                () -> assertInstanceOf(Integer.class, CassandraType.parse("set<int>", "7"))
        );
    }

    @Test
    void shouldFallBackToAnyRatherThanThrowForAnUnrecognisedType() {
        assertAll(
                () -> assertDoesNotThrow(() -> CassandraType.toOpenApiType("something_new")),
                () -> assertEquals("ANY", CassandraType.toOpenApiType("something_new")),
                () -> assertEquals("ANY", CassandraType.toOpenApiType(null))
        );
    }

    @Test
    void shouldParseStringsIntoTheJavaTypesTheDriverBinds() {
        assertAll(
                () -> assertEquals(42, CassandraType.parse("int", "42")),
                () -> assertEquals(42L, CassandraType.parse("bigint", "42")),
                () -> assertEquals((short) 42, CassandraType.parse("smallint", "42")),
                () -> assertEquals((byte) 42, CassandraType.parse("tinyint", "42")),
                () -> assertEquals(new BigInteger("42"), CassandraType.parse("varint", "42")),
                () -> assertEquals(4.5d, CassandraType.parse("double", "4.5")),
                () -> assertEquals(4.5f, CassandraType.parse("float", "4.5")),
                () -> assertEquals(new BigDecimal("4.5"), CassandraType.parse("decimal", "4.5")),
                () -> assertEquals(true, CassandraType.parse("boolean", "true")),
                () -> assertEquals("hello", CassandraType.parse("text", "hello")),
                () -> assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        CassandraType.parse("uuid", "11111111-1111-1111-1111-111111111111")),
                () -> assertEquals(Instant.parse("2023-06-24T10:12:28Z"),
                        CassandraType.parse("timestamp", "2023-06-24T10:12:28Z")),
                () -> assertEquals(LocalDate.parse("2023-06-24"), CassandraType.parse("date", "2023-06-24")),
                () -> assertEquals(LocalTime.parse("10:12:28"), CassandraType.parse("time", "10:12:28"))
        );
    }

    @Test
    void shouldParseIntoDriverTypesRatherThanStrings() {
        // The DataStax driver binds by exact Java type, so these must not come back as Strings.
        assertAll(
                () -> assertInstanceOf(UUID.class, CassandraType.parse("uuid", "11111111-1111-1111-1111-111111111111")),
                () -> assertInstanceOf(Instant.class, CassandraType.parse("timestamp", "2023-06-24T10:12:28Z")),
                () -> assertInstanceOf(LocalDate.class, CassandraType.parse("date", "2023-06-24")),
                () -> assertInstanceOf(InetAddress.class, CassandraType.parse("inet", "127.0.0.1")),
                () -> assertInstanceOf(ByteBuffer.class, CassandraType.parse("blob", "aGk="))
        );
    }

    @Test
    void shouldTreatBlankAndNullValuesAsNull() {
        assertAll(
                () -> assertNull(CassandraType.parse("int", "")),
                () -> assertNull(CassandraType.parse("int", "  ")),
                () -> assertNull(CassandraType.parse("text", null)),
                () -> assertNull(CassandraType.toSqlValue("bigint", null))
        );
    }

    @Test
    void shouldWidenJsonNumbersToTheColumnsExactJavaType() {
        // JSON hands out an Integer for 42, but a bigint column has to be bound as a Long.
        var body = new JsonObject().put("duration_ms", 42).put("release_year", 1959);

        assertAll(
                () -> assertEquals(42L, CassandraType.toSqlValue("bigint", body.getValue("duration_ms"))),
                () -> assertInstanceOf(Long.class, CassandraType.toSqlValue("bigint", body.getValue("duration_ms"))),
                () -> assertEquals(1959, CassandraType.toSqlValue("int", body.getValue("release_year"))),
                () -> assertInstanceOf(Integer.class, CassandraType.toSqlValue("int", body.getValue("release_year")))
        );
    }

    @Test
    void shouldConvertStringBodyValuesIntoDriverTypesOnInsert() {
        var body = new JsonObject()
                .put("album_id", "11111111-1111-1111-1111-111111111111")
                .put("in_print", true);

        assertAll(
                () -> assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        CassandraType.toSqlValue("uuid", body.getValue("album_id"))),
                () -> assertEquals(true, CassandraType.toSqlValue("boolean", body.getValue("in_print")))
        );
    }

    @Test
    void shouldRenderDriverTypesWithoutAJsonRepresentationAsStrings() {
        var uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertAll(
                () -> assertEquals(uuid.toString(), CassandraType.toRowValue("uuid", uuid)),
                () -> assertEquals(uuid.toString(), CassandraType.toRowValue("timeuuid", uuid)),
                () -> assertEquals("2023-06-24T10:12:28Z",
                        CassandraType.toRowValue("timestamp", Instant.parse("2023-06-24T10:12:28Z"))),
                () -> assertEquals("2023-06-24", CassandraType.toRowValue("date", LocalDate.parse("2023-06-24"))),
                () -> assertEquals("10:12:28", CassandraType.toRowValue("time", LocalTime.parse("10:12:28")))
        );
    }

    @Test
    void shouldRenderInetAndBlobColumnsAsStrings() throws Exception {
        var address = InetAddress.getByName("127.0.0.1");
        var blob = ByteBuffer.wrap("hi".getBytes(StandardCharsets.UTF_8));

        assertAll(
                () -> assertEquals("127.0.0.1", CassandraType.toRowValue("inet", address)),
                () -> assertEquals(Base64.getEncoder().encodeToString("hi".getBytes(StandardCharsets.UTF_8)),
                        CassandraType.toRowValue("blob", blob)),
                // Reading a blob must not consume the buffer.
                () -> assertEquals(2, blob.remaining())
        );
    }

    @Test
    void shouldLeaveValuesThatJsonAlreadyUnderstandsUntouched() {
        assertAll(
                () -> assertEquals("hello", CassandraType.toRowValue("text", "hello")),
                () -> assertEquals(42, CassandraType.toRowValue("int", 42)),
                () -> assertEquals(true, CassandraType.toRowValue("boolean", true)),
                () -> assertNull(CassandraType.toRowValue("text", null))
        );
    }
}
