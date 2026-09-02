package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableRequest;
import com.dercio.database_proxy.postgres.InconsistentStateException;
import io.vertx.cassandra.CassandraClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraTableFinderTest {

    @Mock
    private CassandraClient cassandraClient;

    @Test
    void shouldOrderPartitionKeysThenClusteringKeysThenTheRemainingColumns() {
        stubSchema(List.of("tracks"), tracksColumns());

        var tables = new CassandraTableFinder(cassandraClient).findTables("music").result();
        var columns = tables.getFirst().getTableMetadata().getColumnNames();

        // duration_ms and title are regular columns, so they sort alphabetically after the key.
        assertEquals(List.of("album_id", "track_no", "duration_ms", "title"), columns);
    }

    @Test
    void shouldFlagPartitionAndClusteringColumnsAsThePrimaryKey() {
        stubSchema(List.of("tracks"), tracksColumns());

        var table = new CassandraTableFinder(cassandraClient).findTables("music").result().getFirst();

        assertAll(
                () -> assertEquals(List.of("album_id", "track_no"),
                        table.getTableMetadata().getPrimaryKeyColumnNames()),
                () -> assertEquals(List.of("duration_ms", "title"), table.getNonPrimaryKeyColumns()
                        .stream().map(ColumnMetadata::getColumnName).toList()),
                // Only a fully restricted partition key plus a clustering prefix avoids a scan.
                () -> assertFalse(table.requiresAllowFiltering(List.of("album_id", "track_no"))),
                () -> assertTrue(table.requiresAllowFiltering(List.of("track_no")))
        );
    }

    @Test
    void shouldResolveOpenApiTypesUsingCqlTypeNames() {
        stubSchema(List.of("tracks"), tracksColumns());

        var columns = new CassandraTableFinder(cassandraClient).findTables("music").result()
                .getFirst().getColumns();

        assertAll(
                () -> assertEquals("string", openApiTypeOf(columns, "album_id")),
                () -> assertEquals("integer", openApiTypeOf(columns, "track_no")),
                () -> assertEquals("integer", openApiTypeOf(columns, "duration_ms")),
                () -> assertEquals("string", openApiTypeOf(columns, "title"))
        );
    }

    @Test
    void shouldMarkKeyColumnsAsNotNullable() {
        stubSchema(List.of("tracks"), tracksColumns());

        var columns = new CassandraTableFinder(cassandraClient).findTables("music").result()
                .getFirst().getColumns();

        assertAll(
                () -> assertFalse(columnNamed(columns, "album_id").isNullable()),
                () -> assertTrue(columnNamed(columns, "title").isNullable())
        );
    }

    @Test
    void shouldIgnoreColumnsBelongingToMaterializedViews() {
        // system_schema.columns also describes views, which cannot be written through.
        var columns = new ArrayList<>(tracksColumns());
        columns.add(schemaRow("tracks_by_title", "title", "text", "partition_key", 0));

        stubSchema(List.of("tracks"), columns);

        var tables = new CassandraTableFinder(cassandraClient).findTables("music").result();

        assertAll(
                () -> assertEquals(1, tables.size()),
                () -> assertEquals("tracks", tables.getFirst().getTableName())
        );
    }

    @Test
    void shouldServeARepeatedLookupFromTheCacheWithoutQueryingAgain() {
        stubSchema(List.of("tracks"), tracksColumns());
        var finder = new CassandraTableFinder(cassandraClient);
        var request = new TableRequest("music", "music", "tracks", Map.of(), Map.of(), null);

        finder.findTable(request);
        var cached = finder.findTable(request);

        assertAll(
                () -> assertEquals("tracks", cached.result().getTableName()),
                // Two statements for the first lookup (tables then columns) and none for the second.
                () -> verify(cassandraClient, times(2)).prepare(anyString())
        );
    }

    @Test
    void shouldFailWhenTheRequestedTableDoesNotExist() {
        stubSchema(List.of("tracks"), tracksColumns());

        var result = new CassandraTableFinder(cassandraClient)
                .findTable(new TableRequest("music", "music", "missing", Map.of(), Map.of(), null));

        assertAll(
                () -> assertTrue(result.failed()),
                () -> assertInstanceOf(InconsistentStateException.class, result.cause())
        );
    }

    private void stubSchema(List<String> tableNames, List<Row> columns) {
        var tableRows = tableNames.stream().map(this::tableRow).toList();

        CassandraClientRecorder.recordSequence(cassandraClient, tableRows, columns);
    }

    private Row tableRow(String tableName) {
        var row = mock(Row.class);
        when(row.getString("table_name")).thenReturn(tableName);
        return row;
    }

    private List<Row> tracksColumns() {
        return List.of(
                // Deliberately out of order - the finder is what imposes the key order.
                schemaRow("tracks", "title", "text", "regular", -1),
                schemaRow("tracks", "track_no", "int", "clustering", 0),
                schemaRow("tracks", "duration_ms", "bigint", "regular", -1),
                schemaRow("tracks", "album_id", "uuid", "partition_key", 0)
        );
    }

    // Lenient because a row belonging to a materialized view is discarded on table_name alone, so
    // its remaining columns are never read.
    private Row schemaRow(String tableName, String columnName, String type, String kind, int position) {
        var row = mock(Row.class);
        lenient().when(row.getString("table_name")).thenReturn(tableName);
        lenient().when(row.getString("column_name")).thenReturn(columnName);
        lenient().when(row.getString("type")).thenReturn(type);
        lenient().when(row.getString("kind")).thenReturn(kind);
        lenient().when(row.getInt("position")).thenReturn(position);
        return row;
    }

    private String openApiTypeOf(List<ColumnMetadata> columns, String columnName) {
        return columnNamed(columns, columnName).getOpenApiType();
    }

    private ColumnMetadata columnNamed(List<ColumnMetadata> columns, String columnName) {
        return columns.stream()
                .filter(column -> columnName.equals(column.getColumnName()))
                .findFirst()
                .orElseThrow();
    }
}
