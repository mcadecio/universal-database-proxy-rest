package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.dercio.database_proxy.cassandra.type.CassandraType;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraObjectInserterTest {

    private static final UUID ALBUM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private CassandraClient cassandraClient;

    @Mock
    private CassandraObjectFinder finder;

    @Test
    void createShouldInsertEveryColumnWithoutAReturningClause() {
        when(cassandraClient.executeWithFullFetch(any(SimpleStatement.class)))
                .thenReturn(Future.succeededFuture(List.of()));
        var captor = ArgumentCaptor.forClass(SimpleStatement.class);

        new CassandraObjectInserter(cassandraClient, finder).create(albums(), albumBody());

        verify(cassandraClient).executeWithFullFetch(captor.capture());
        var query = captor.getValue().getQuery();

        assertAll(
                () -> assertEquals("INSERT INTO music.albums(album_id, title, artist) VALUES (?,?,?)", query),
                // CQL has no RETURNING - the Location id is rebuilt from the body instead.
                () -> assertFalse(query.contains("RETURNING")),
                () -> assertEquals(List.of(ALBUM_ID, "Kind of Blue", "Miles Davis"),
                        captor.getValue().getPositionalValues())
        );
    }

    @Test
    void createShouldBuildTheResourceIdFromThePrimaryKeyValuesInTheBody() {
        when(cassandraClient.executeWithFullFetch(any(SimpleStatement.class)))
                .thenReturn(Future.succeededFuture(List.of()));

        var created = new CassandraObjectInserter(cassandraClient, finder).create(albums(), albumBody());

        assertEquals(ALBUM_ID.toString(), created.result());
    }

    @Test
    void createShouldJoinCompositePrimaryKeyValuesWithAColon() {
        when(cassandraClient.executeWithFullFetch(any(SimpleStatement.class)))
                .thenReturn(Future.succeededFuture(List.of()));

        var body = new JsonObject()
                .put("album_id", ALBUM_ID.toString())
                .put("track_no", 2)
                .put("title", "Freddie Freeloader");

        var created = new CassandraObjectInserter(cassandraClient, finder).create(tracks(), body);

        assertEquals(ALBUM_ID + ":2", created.result());
    }

    @Test
    void updateShouldNotIncludeThePrimaryKeyInTheSetClauseAndBindsPkFromPath() {
        when(finder.existsByPrimaryKey(any(), any())).thenReturn(Future.succeededFuture(true));
        when(cassandraClient.executeWithFullFetch(any(SimpleStatement.class)))
                .thenReturn(Future.succeededFuture(List.of()));
        var captor = ArgumentCaptor.forClass(SimpleStatement.class);

        // The body carries a stale key that must be ignored; the key comes from the path.
        var body = new JsonObject()
                .put("album_id", "99999999-9999-9999-9999-999999999999")
                .put("title", "Kind of Blue")
                .put("artist", "Miles Davis");

        var updated = new CassandraObjectInserter(cassandraClient, finder)
                .update(albums(), body, Map.of("album_id", ALBUM_ID.toString()));

        verify(cassandraClient).executeWithFullFetch(captor.capture());
        var query = captor.getValue().getQuery();

        assertAll(
                () -> assertEquals("UPDATE music.albums SET title = ?, artist = ? WHERE album_id = ?", query),
                () -> assertFalse(query.split("SET")[1].split("WHERE")[0].contains("album_id")),
                () -> assertEquals(List.of("Kind of Blue", "Miles Davis", ALBUM_ID),
                        captor.getValue().getPositionalValues()),
                () -> assertEquals(1, updated.result())
        );
    }

    @Test
    void updateShouldReportNoRowsWhenTheReadBeforeWriteFindsNothing() {
        // Cassandra reports no affected-row count and INSERT is an upsert, so without this the
        // request would silently create the row and answer 204 instead of 404.
        when(finder.existsByPrimaryKey(any(), any())).thenReturn(Future.succeededFuture(false));

        var updated = new CassandraObjectInserter(cassandraClient, finder)
                .update(albums(), albumBody(), Map.of("album_id", ALBUM_ID.toString()));

        assertAll(
                () -> assertEquals(0, updated.result()),
                () -> verify(cassandraClient, never()).executeWithFullFetch(any(SimpleStatement.class))
        );
    }

    @Test
    void updateShouldIssueNoStatementForTablesWhereEveryColumnIsPartOfThePrimaryKey() {
        // There is nothing to SET, and CQL cannot assign a key column to itself the way Postgres
        // does, so the existence check alone answers the request.
        when(finder.existsByPrimaryKey(any(), any())).thenReturn(Future.succeededFuture(true));

        var updated = new CassandraObjectInserter(cassandraClient, finder)
                .update(genres(), new JsonObject().put("name", "jazz"), Map.of("name", "jazz"));

        assertAll(
                () -> assertEquals(1, updated.result()),
                () -> verify(cassandraClient, never()).executeWithFullFetch(any(SimpleStatement.class))
        );
    }

    @Test
    void updateShouldRestrictByEveryColumnOfACompositePrimaryKey() {
        when(finder.existsByPrimaryKey(any(), any())).thenReturn(Future.succeededFuture(true));
        when(cassandraClient.executeWithFullFetch(any(SimpleStatement.class)))
                .thenReturn(Future.succeededFuture(List.of(mock(Row.class))));
        var captor = ArgumentCaptor.forClass(SimpleStatement.class);

        var body = new JsonObject().put("title", "So What").put("duration_ms", 562000);
        var pathParams = Map.of("album_id", ALBUM_ID.toString(), "track_no", "1");

        new CassandraObjectInserter(cassandraClient, finder).update(tracks(), body, pathParams);

        verify(cassandraClient).executeWithFullFetch(captor.capture());

        assertAll(
                () -> assertTrue(captor.getValue().getQuery()
                        .endsWith("WHERE album_id = ? AND track_no = ?")),
                () -> assertEquals(List.of("So What", 562000L, ALBUM_ID, 1),
                        captor.getValue().getPositionalValues())
        );
    }

    private JsonObject albumBody() {
        return new JsonObject()
                .put("album_id", ALBUM_ID.toString())
                .put("title", "Kind of Blue")
                .put("artist", "Miles Davis");
    }

    private CassandraTableMetadata albums() {
        var tableMetadata = new TableMetadata("music", "music", "albums", List.of(
                column("albums", "album_id", "uuid", true),
                column("albums", "title", "text", false),
                column("albums", "artist", "text", false)
        ));

        return new CassandraTableMetadata(tableMetadata, List.of("album_id"), List.of());
    }

    private CassandraTableMetadata tracks() {
        var tableMetadata = new TableMetadata("music", "music", "tracks", List.of(
                column("tracks", "album_id", "uuid", true),
                column("tracks", "track_no", "int", true),
                column("tracks", "title", "text", false),
                column("tracks", "duration_ms", "bigint", false)
        ));

        return new CassandraTableMetadata(tableMetadata, List.of("album_id"), List.of("track_no"));
    }

    private CassandraTableMetadata genres() {
        var tableMetadata = new TableMetadata("music", "music", "genres", List.of(
                column("genres", "name", "text", true)
        ));

        return new CassandraTableMetadata(tableMetadata, List.of("name"), List.of());
    }

    private ColumnMetadata column(String table, String name, String dataType, boolean primaryKey) {
        return new ColumnMetadata(new JsonObject()
                .put("table_schema", "music")
                .put("table_name", table)
                .put("column_name", name)
                .put("data_type", dataType)
                .put("is_nullable", primaryKey ? "NO" : "YES")
                .put("is_primary_key", primaryKey), CassandraType::toOpenApiType);
    }
}
