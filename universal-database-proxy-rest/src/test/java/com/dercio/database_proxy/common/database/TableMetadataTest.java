package com.dercio.database_proxy.common.database;

import com.dercio.database_proxy.postgres.type.PgType;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableMetadataTest {

    @Test
    void shouldMoveTheDeclaredPrimaryKeyColumnToTheFront() {
        var tableMetadata = new TableMetadata("db", "vehicles", "cars", List.of(
                column("manufacturer", "character varying", false),
                column("car_id", "bigint", true),
                column("doors", "bigint", false)
        ));

        assertAll(
                () -> assertEquals(List.of("car_id", "manufacturer", "doors"), tableMetadata.getColumnNames()),
                () -> assertEquals("car_id", tableMetadata.getPkColumnName()),
                () -> assertEquals(List.of("car_id"), tableMetadata.getPrimaryKeyColumnNames()),
                () -> assertEquals(List.of("manufacturer", "doors"),
                        tableMetadata.getNonPrimaryKeyColumns().stream().map(ColumnMetadata::getColumnName).toList()),
                () -> assertEquals(3, tableMetadata.getNumberOfColumns())
        );
    }

    @Test
    void shouldOrderAllPrimaryKeyColumnsFirstForCompositeKeys() {
        var tableMetadata = new TableMetadata("db", "public", "enrolments", List.of(
                column("grade", "text", false),
                column("student_id", "bigint", true),
                column("course_id", "bigint", true)
        ));

        assertAll(
                () -> assertEquals(List.of("student_id", "course_id", "grade"), tableMetadata.getColumnNames()),
                () -> assertEquals(List.of("student_id", "course_id"), tableMetadata.getPrimaryKeyColumnNames()),
                () -> assertEquals(List.of("grade"),
                        tableMetadata.getNonPrimaryKeyColumns().stream().map(ColumnMetadata::getColumnName).toList()),
                () -> assertEquals(3, tableMetadata.getNumberOfColumns())
        );
    }

    @Test
    void shouldPromoteTheFirstColumnToPrimaryKeyWhenNoneIsDeclared() {
        var tableMetadata = new TableMetadata("db", "public", "logs", List.of(
                column("event", "text", false),
                column("created_at", "timestamp with time zone", false)
        ));

        assertAll(
                () -> assertEquals("event", tableMetadata.getPkColumnName()),
                () -> assertTrue(tableMetadata.getColumns().getFirst().isPrimaryKey()),
                () -> assertEquals(List.of("event"), tableMetadata.getPrimaryKeyColumnNames())
        );
    }

    @Test
    void shouldNotFailWhenPrimaryKeyFlagIsMissingFromMetadata() {
        var rawColumn = new JsonObject()
                .put("table_schema", "public")
                .put("table_name", "notes")
                .put("column_name", "id")
                .put("data_type", "bigint")
                .put("is_nullable", "NO");

        var column = assertDoesNotThrow(() -> new ColumnMetadata(rawColumn, PgType::toOpenApiColumnType));

        assertFalse(column.isPrimaryKey());
    }

    private ColumnMetadata column(String name, String dataType, boolean primaryKey) {
        return new ColumnMetadata(new JsonObject()
                .put("table_schema", "vehicles")
                .put("table_name", "cars")
                .put("column_name", name)
                .put("data_type", dataType)
                .put("is_nullable", "NO")
                .put("is_primary_key", primaryKey), PgType::toOpenApiColumnType);
    }
}
