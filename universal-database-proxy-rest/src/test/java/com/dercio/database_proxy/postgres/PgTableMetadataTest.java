package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PgTableMetadataTest {

    @Test
    void shouldBuildTheQualifiedTableName() {
        var metadata = new PgTableMetadata(carsTable());

        assertEquals("vehicles.cars", metadata.getQualifiedTableName());
    }

    @Test
    void shouldParseOnlyRawValuesThatMatchKnownColumnsInColumnOrder() {
        var metadata = new PgTableMetadata(carsTable());

        // "car_id" is the primary key so it is ordered first; "unknown" must be ignored.
        var rawValues = new LinkedHashMap<String, String>();
        rawValues.put("manufacturer", "toyota");
        rawValues.put("car_id", "5");
        rawValues.put("unknown", "ignored");

        Tuple tuple = metadata.parseRawValues(rawValues);

        assertAll(
                () -> assertEquals(2, tuple.size()),
                () -> assertEquals(5L, tuple.getValue(0)),
                () -> assertEquals("toyota", tuple.getValue(1))
        );
    }

    @Test
    void shouldReturnAnEmptyTupleWhenThereAreNoRawValues() {
        var metadata = new PgTableMetadata(carsTable());

        assertEquals(0, metadata.parseRawValues(Map.of()).size());
    }

    @Test
    void shouldGenerateInsertTupleWithEveryColumnValueFromTheBody() {
        var metadata = new PgTableMetadata(simpleTable());

        var body = new JsonObject().put("id", 1).put("name", "alice");

        Tuple tuple = metadata.generateTupleForInsert(body);

        assertAll(
                () -> assertEquals(2, tuple.size()),
                () -> assertEquals(1, tuple.getValue(0)),
                () -> assertEquals("alice", tuple.getValue(1))
        );
    }

    @Test
    void shouldGenerateUpdateTupleWithNonPkBodyValuesFollowedByPkPathParams() {
        var metadata = new PgTableMetadata(simpleTable());

        // The body carries a stale "id" that must be ignored; the PK comes from the path.
        var body = new JsonObject().put("id", 1).put("name", "alice");
        var pathParams = Map.of("id", "9");

        Tuple tuple = metadata.generateTupleForUpdate(body, pathParams);

        assertAll(
                () -> assertEquals(2, tuple.size()),
                () -> assertEquals("alice", tuple.getValue(0)),
                () -> assertEquals(9L, tuple.getValue(1))
        );
    }

    @Test
    void shouldNormalizeRowsSkippingNullsAndCoercingJson() {
        var metadata = new PgTableMetadata(carsTable());

        var row = new JsonObject()
                .put("car_id", 7)
                .put("manufacturer", "toyota")
                .put("extra_details", "{\"colour\":\"red\"}");
        // "doors" and "last_updated" are absent -> must not appear in the output.

        JsonObject normalized = metadata.normalizeRow(row);

        assertAll(
                () -> assertEquals(7, normalized.getInteger("car_id")),
                () -> assertEquals("toyota", normalized.getString("manufacturer")),
                () -> assertInstanceOf(JsonObject.class, normalized.getValue("extra_details")),
                () -> assertEquals("red", normalized.getJsonObject("extra_details").getString("colour")),
                () -> assertFalse(normalized.containsKey("doors")),
                () -> assertFalse(normalized.containsKey("last_updated"))
        );
    }

    private TableMetadata carsTable() {
        return new TableMetadata("defaultdb", "vehicles", "cars", List.of(
                column("vehicles", "cars", "manufacturer", "character varying", false),
                column("vehicles", "cars", "car_id", "bigint", true),
                column("vehicles", "cars", "doors", "bigint", false),
                column("vehicles", "cars", "last_updated", "timestamp with time zone", false),
                column("vehicles", "cars", "extra_details", "jsonb", false)
        ));
    }

    private TableMetadata simpleTable() {
        return new TableMetadata("db", "public", "people", List.of(
                column("public", "people", "id", "bigint", true),
                column("public", "people", "name", "text", false)
        ));
    }

    private ColumnMetadata column(String schema, String table, String name, String dataType, boolean primaryKey) {
        return new ColumnMetadata(new JsonObject()
                .put("table_schema", schema)
                .put("table_name", table)
                .put("column_name", name)
                .put("data_type", dataType)
                .put("is_nullable", "YES")
                .put("is_primary_key", primaryKey));
    }
}
