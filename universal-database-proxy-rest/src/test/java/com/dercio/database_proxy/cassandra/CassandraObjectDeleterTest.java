package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.dercio.database_proxy.cassandra.type.CassandraType;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraObjectDeleterTest {

    private static final UUID FIRST_ALBUM = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_ALBUM = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private CassandraClient cassandraClient;

    @Mock
    private CassandraObjectFinder finder;

    @Test
    void shouldTruncateWhenDeletingTheWholeCollection() {
        // CQL rejects a bare "DELETE FROM table", so clearing a table means TRUNCATE.
        var recorder = CassandraClientRecorder.record(cassandraClient);

        var deleted = new CassandraObjectDeleter(cassandraClient, finder).deleteData(albums(), Map.of());

        assertAll(
                () -> assertEquals("TRUNCATE music.albums", recorder.query()),
                // TRUNCATE reports no count, so this always answers 204 rather than 404.
                () -> assertEquals(1, deleted.result())
        );
    }

    @Test
    void shouldTruncateWhenEveryFilterNameIsUnknown() {
        var recorder = CassandraClientRecorder.record(cassandraClient);

        new CassandraObjectDeleter(cassandraClient, finder)
                .deleteData(albums(), Map.of("not_a_column", "boom"));

        assertEquals("TRUNCATE music.albums", recorder.query());
    }

    @Test
    void shouldDeleteEachMatchingRowByItsPrimaryKeyWhenFiltered() {
        // Stub the rows before the outer when(...), or Mockito sees nested stubbing.
        var rows = List.of(rowWith(FIRST_ALBUM), rowWith(SECOND_ALBUM));
        when(finder.findRows(any(), any())).thenReturn(Future.succeededFuture(rows));
        var recorder = CassandraClientRecorder.record(cassandraClient);

        var deleted = new CassandraObjectDeleter(cassandraClient, finder)
                .deleteData(albums(), Map.of("artist", "Miles Davis"));

        assertAll(
                () -> assertEquals(2, deleted.result()),
                () -> assertTrue(recorder.queries().stream()
                        .allMatch("DELETE FROM music.albums WHERE album_id = ?"::equals)),
                () -> assertEquals(List.of(FIRST_ALBUM), recorder.allValues().getFirst()),
                () -> assertEquals(List.of(SECOND_ALBUM), recorder.allValues().get(1))
        );
    }

    @Test
    void shouldReportNoRowsWhenAFilteredDeleteMatchesNothing() {
        when(finder.findRows(any(), any())).thenReturn(Future.succeededFuture(List.of()));

        var deleted = new CassandraObjectDeleter(cassandraClient, finder)
                .deleteData(albums(), Map.of("artist", "Nobody"));

        assertAll(
                () -> assertEquals(0, deleted.result()),
                () -> verify(cassandraClient, never()).prepare(anyString())
        );
    }

    @Test
    void deleteByIdShouldReportNoRowsWhenTheReadBeforeWriteFindsNothing() {
        // A DELETE by primary key always reports success in CQL, so the read is what produces a 404.
        when(finder.existsByPrimaryKey(any(), any())).thenReturn(Future.succeededFuture(false));

        var deleted = new CassandraObjectDeleter(cassandraClient, finder)
                .deleteDataById(albums(), Map.of("album_id", FIRST_ALBUM.toString()));

        assertAll(
                () -> assertEquals(0, deleted.result()),
                () -> verify(cassandraClient, never()).prepare(anyString())
        );
    }

    @Test
    void deleteByIdShouldRestrictByEveryColumnOfACompositePrimaryKey() {
        when(finder.existsByPrimaryKey(any(), any())).thenReturn(Future.succeededFuture(true));
        var recorder = CassandraClientRecorder.record(cassandraClient);

        var deleted = new CassandraObjectDeleter(cassandraClient, finder)
                .deleteDataById(tracks(), Map.of("album_id", FIRST_ALBUM.toString(), "track_no", "2"));

        assertAll(
                () -> assertEquals("DELETE FROM music.tracks WHERE album_id = ? AND track_no = ?",
                        recorder.query()),
                () -> assertEquals(List.of(FIRST_ALBUM, 2), recorder.values()),
                () -> assertEquals(1, deleted.result())
        );
    }

    private Row rowWith(UUID albumId) {
        var row = mock(Row.class);
        when(row.getObject("album_id")).thenReturn(albumId);
        return row;
    }

    private CassandraTableMetadata albums() {
        var tableMetadata = new TableMetadata("music", "music", "albums", List.of(
                column("albums", "album_id", "uuid", true),
                column("albums", "artist", "text", false)
        ));

        return new CassandraTableMetadata(tableMetadata, List.of("album_id"), List.of());
    }

    private CassandraTableMetadata tracks() {
        var tableMetadata = new TableMetadata("music", "music", "tracks", List.of(
                column("tracks", "album_id", "uuid", true),
                column("tracks", "track_no", "int", true),
                column("tracks", "title", "text", false)
        ));

        return new CassandraTableMetadata(tableMetadata, List.of("album_id"), List.of("track_no"));
    }

    private ColumnMetadata column(String table, String name, String dataType, boolean primaryKey) {
        return new ColumnMetadata(new JsonObject()
                .put("table_schema", "music")
                .put("table_name", table)
                .put("column_name", name)
                .put("data_type", dataType)
                .put("is_nullable", primaryKey ? "NO" : "YES")
                .put("is_primary_key", primaryKey), CassandraType::toOpenApiColumnType);
    }
}
