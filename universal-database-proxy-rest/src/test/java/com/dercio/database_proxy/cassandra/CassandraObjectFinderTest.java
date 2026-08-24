package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.dercio.database_proxy.cassandra.type.CassandraType;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.postgres.InconsistentStateException;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraObjectFinderTest {

    private static final UUID ALBUM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private CassandraClient cassandraClient;

    @Test
    void shouldSelectEveryRowWhenThereAreNoFilters() {
        var statement = captureStatementFor(Map.of(), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums", statement.getQuery()),
                () -> assertTrue(statement.getPositionalValues().isEmpty())
        );
    }

    @Test
    void shouldNotUseAllowFilteringWhenTheFilterIsSatisfiedByTheKey() {
        var statement = captureStatementFor(Map.of("album_id", ALBUM_ID.toString()), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums WHERE album_id = ?", statement.getQuery()),
                () -> assertFalse(statement.getQuery().contains("ALLOW FILTERING")),
                () -> assertEquals(1, statement.getPositionalValues().size()),
                () -> assertEquals(ALBUM_ID, statement.getPositionalValues().get(0)),
                () -> assertInstanceOf(UUID.class, statement.getPositionalValues().get(0))
        );
    }

    @Test
    void shouldAppendAllowFilteringForANonKeyColumn() {
        var statement = captureStatementFor(Map.of("artist", "Miles Davis"), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums WHERE artist = ? ALLOW FILTERING", statement.getQuery()),
                () -> assertEquals(List.of("Miles Davis"), statement.getPositionalValues())
        );
    }

    @Test
    void shouldFilterSetColumnsByMembershipRatherThanEquality() {
        // Equality would require the caller to spell out the whole set in the query string.
        var statement = captureStatementFor(Map.of("tags", "jazz"), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums WHERE tags CONTAINS ? ALLOW FILTERING",
                        statement.getQuery()),
                // CONTAINS binds one element, not a set.
                () -> assertEquals(List.of("jazz"), statement.getPositionalValues())
        );
    }

    @Test
    void shouldFilterSetElementsAsTheirDriverType() {
        var statement = captureStatementFor(Map.of("ratings", "5"), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums WHERE ratings CONTAINS ? ALLOW FILTERING",
                        statement.getQuery()),
                () -> assertEquals(List.of(5), statement.getPositionalValues()),
                () -> assertInstanceOf(Integer.class, statement.getPositionalValues().get(0))
        );
    }

    @Test
    void shouldCombineASetMembershipFilterWithAKeyRestriction() {
        var statement = captureStatementFor(new LinkedHashMap<>(Map.of(
                "album_id", ALBUM_ID.toString(),
                "tags", "jazz"
        )), true);

        assertAll(
                () -> assertEquals(
                        "SELECT * FROM music.albums WHERE album_id = ? AND tags CONTAINS ? ALLOW FILTERING",
                        statement.getQuery()),
                () -> assertEquals(List.of(ALBUM_ID, "jazz"), statement.getPositionalValues())
        );
    }

    @Test
    void shouldRejectASetMembershipFilterWhenAllowFilteringIsDisabled() {
        // CONTAINS always needs a scan, so it must be refused just like any other non-key filter.
        var result = new CassandraObjectFinder(cassandraClient, false).find(albums(), Map.of("tags", "jazz"));

        assertAll(
                () -> assertTrue(result.failed()),
                () -> assertInstanceOf(InconsistentStateException.class, result.cause()),
                () -> verify(cassandraClient, never()).executeWithFullFetch(any(SimpleStatement.class))
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
                () -> verify(cassandraClient, never()).executeWithFullFetch(any(SimpleStatement.class))
        );
    }

    @Test
    void shouldDropFilterNamesThatAreNotRealColumns() {
        // The whitelist is what keeps caller-supplied names out of the generated CQL.
        var statement = captureStatementFor(Map.of("definitely_not_a_column", "boom"), true);

        assertAll(
                () -> assertEquals("SELECT * FROM music.albums", statement.getQuery()),
                () -> assertTrue(statement.getPositionalValues().isEmpty())
        );
    }

    @Test
    void shouldCheckExistenceWithASinglePartitionReadOfThePrimaryKey() {
        when(cassandraClient.executeWithFullFetch(any(SimpleStatement.class)))
                .thenReturn(Future.succeededFuture(List.of()));
        var captor = ArgumentCaptor.forClass(SimpleStatement.class);

        var exists = new CassandraObjectFinder(cassandraClient, true)
                .existsByPrimaryKey(albums(), Map.of("album_id", ALBUM_ID.toString()));

        verify(cassandraClient).executeWithFullFetch(captor.capture());

        assertAll(
                () -> assertEquals("SELECT album_id FROM music.albums WHERE album_id = ? LIMIT 1",
                        captor.getValue().getQuery()),
                () -> assertFalse(captor.getValue().getQuery().contains("ALLOW FILTERING")),
                () -> assertEquals(List.of(ALBUM_ID), captor.getValue().getPositionalValues()),
                () -> assertEquals(Boolean.FALSE, exists.result())
        );
    }

    @Test
    void shouldReportExistenceWhenTheReadReturnsARow() {
        when(cassandraClient.executeWithFullFetch(any(SimpleStatement.class)))
                .thenReturn(Future.succeededFuture(List.of(mock(Row.class))));

        var exists = new CassandraObjectFinder(cassandraClient, true)
                .existsByPrimaryKey(albums(), Map.of("album_id", ALBUM_ID.toString()));

        assertEquals(Boolean.TRUE, exists.result());
    }

    private SimpleStatement captureStatementFor(Map<String, String> filters, boolean allowFiltering) {
        when(cassandraClient.executeWithFullFetch(any(SimpleStatement.class)))
                .thenReturn(Future.succeededFuture(List.of()));
        var captor = ArgumentCaptor.forClass(SimpleStatement.class);

        new CassandraObjectFinder(cassandraClient, allowFiltering).find(albums(), filters);

        verify(cassandraClient).executeWithFullFetch(captor.capture());

        return captor.getValue();
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
