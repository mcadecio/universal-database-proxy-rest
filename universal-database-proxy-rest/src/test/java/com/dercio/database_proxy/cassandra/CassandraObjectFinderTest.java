package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.dercio.database_proxy.cassandra.type.CassandraType;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.postgres.InconsistentStateException;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraObjectFinderTest {

    private static final UUID ALBUM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private CassandraClient cassandraClient;

    @Test
    void shouldSelectEveryRowWhenThereAreNoFilters() {
        var recorder = captureStatementFor(Map.of(), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums", recorder.query()),
                () -> assertTrue(recorder.values().isEmpty())
        );
    }

    @Test
    void shouldNotUseAllowFilteringWhenTheFilterIsSatisfiedByTheKey() {
        var recorder = captureStatementFor(Map.of("album_id", ALBUM_ID.toString()), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums WHERE album_id = ?", recorder.query()),
                () -> assertFalse(recorder.query().contains("ALLOW FILTERING")),
                () -> assertEquals(1, recorder.values().size()),
                () -> assertEquals(ALBUM_ID, recorder.values().getFirst()),
                () -> assertInstanceOf(UUID.class, recorder.values().getFirst())
        );
    }

    @Test
    void shouldAppendAllowFilteringForANonKeyColumn() {
        var recorder = captureStatementFor(Map.of("artist", "Miles Davis"), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums WHERE artist = ? ALLOW FILTERING", recorder.query()),
                () -> assertEquals(List.of("Miles Davis"), recorder.values())
        );
    }

    @Test
    void shouldFilterSetColumnsByMembershipRatherThanEquality() {
        // Equality would require the caller to spell out the whole set in the query string.
        var recorder = captureStatementFor(Map.of("tags", "jazz"), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums WHERE tags CONTAINS ? ALLOW FILTERING",
                        recorder.query()),
                // CONTAINS binds one element, not a set.
                () -> assertEquals(List.of("jazz"), recorder.values())
        );
    }

    @Test
    void shouldFilterSetElementsAsTheirDriverType() {
        var recorder = captureStatementFor(Map.of("ratings", "5"), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums WHERE ratings CONTAINS ? ALLOW FILTERING",
                        recorder.query()),
                () -> assertEquals(List.of(5), recorder.values()),
                () -> assertInstanceOf(Integer.class, recorder.values().getFirst())
        );
    }

    @Test
    void shouldCombineASetMembershipFilterWithAKeyRestriction() {
        var filters = new LinkedHashMap<String, String>();
        filters.put("album_id", ALBUM_ID.toString());
        filters.put("tags", "jazz");

        var recorder = captureStatementFor(filters, true);

        assertAll(
                () -> assertEquals(
                        "SELECT * FROM music.albums WHERE album_id = ? AND tags CONTAINS ? ALLOW FILTERING",
                        recorder.query()),
                () -> assertEquals(List.of(ALBUM_ID, "jazz"), recorder.values())
        );
    }

    @Test
    void shouldRejectASetMembershipFilterWhenAllowFilteringIsDisabled() {
        // CONTAINS always needs a scan, so it must be refused just like any other non-key filter.
        var result = new CassandraObjectFinder(cassandraClient, false).find(albums(), Map.of("tags", "jazz"));

        assertAll(
                () -> assertTrue(result.failed()),
                () -> assertInstanceOf(InconsistentStateException.class, result.cause()),
                () -> verify(cassandraClient, never()).prepare(anyString())
        );
    }

    @Test
    void shouldFailInsteadOfScanningWhenAllowFilteringIsDisabled() {
        var finder = new CassandraObjectFinder(cassandraClient, false);

        var result = finder.find(albums(), Map.of("artist", "Miles Davis"));

        assertAll(
                () -> assertTrue(result.failed()),
                () -> assertInstanceOf(InconsistentStateException.class, result.cause()),
                () -> assertTrue(result.cause().getMessage().contains("music.albums")),
                () -> verify(cassandraClient, never()).prepare(anyString())
        );
    }

    @Test
    void shouldDropFilterNamesThatAreNotRealColumns() {
        // The whitelist is what keeps caller-supplied names out of the generated CQL.
        var recorder = captureStatementFor(Map.of("definitely_not_a_column", "boom"), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums", recorder.query()),
                () -> assertTrue(recorder.values().isEmpty())
        );
    }

    @Test
    void shouldCheckExistenceWithASinglePartitionReadOfThePrimaryKey() {
        var recorder = CassandraClientRecorder.record(cassandraClient);

        var exists = new CassandraObjectFinder(cassandraClient, true)
                .existsByPrimaryKey(albums(), Map.of("album_id", ALBUM_ID.toString()));

        assertAll(
                () -> assertEquals("SELECT album_id FROM music.albums WHERE album_id = ? LIMIT 1",
                        recorder.query()),
                () -> assertFalse(recorder.query().contains("ALLOW FILTERING")),
                () -> assertEquals(List.of(ALBUM_ID), recorder.values()),
                () -> assertEquals(Boolean.FALSE, exists.result())
        );
    }

    @Test
    void shouldReportExistenceWhenTheReadReturnsARow() {
        CassandraClientRecorder.record(cassandraClient, List.of(mock(Row.class)));

        var exists = new CassandraObjectFinder(cassandraClient, true)
                .existsByPrimaryKey(albums(), Map.of("album_id", ALBUM_ID.toString()));

        assertEquals(Boolean.TRUE, exists.result());
    }

    private CassandraClientRecorder captureStatementFor(Map<String, String> filters, boolean allowFiltering) {
        var recorder = CassandraClientRecorder.record(cassandraClient);

        new CassandraObjectFinder(cassandraClient, allowFiltering).find(albums(), filters);

        return recorder;
    }

    private CassandraTableMetadata albums() {
        var tableMetadata = new TableMetadata("music", "music", "albums", List.of(
                column("album_id", "uuid", true),
                column("title", "text", false),
                column("artist", "text", false),
                column("tags", "set<text>", false),
                column("ratings", "set<int>", false)
        ));

        return new CassandraTableMetadata(tableMetadata, List.of("album_id"), List.of());
    }

    private ColumnMetadata column(String name, String dataType, boolean primaryKey) {
        return new ColumnMetadata(new JsonObject()
                .put("table_schema", "music")
                .put("table_name", "albums")
                .put("column_name", name)
                .put("data_type", dataType)
                .put("is_nullable", primaryKey ? "NO" : "YES")
                .put("is_primary_key", primaryKey), CassandraType::toOpenApiColumnType);
    }
}
