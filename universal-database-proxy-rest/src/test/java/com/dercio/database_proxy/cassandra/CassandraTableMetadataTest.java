package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.Row;
import com.dercio.database_proxy.cassandra.type.CassandraType;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CassandraTableMetadataTest {

    private static final UUID ALBUM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private Row row;

    @Mock
    private ColumnDefinitions columnDefinitions;

    @Test
    void shouldBuildTheQualifiedTableNameFromTheKeyspace() {
        assertEquals("music.albums", albums().getQualifiedTableName());
    }

    @Test
    void shouldWhitelistFilterNamesAgainstRealColumnsInColumnOrder() {
        var filterable = albums().filterableColumnNames(List.of("artist", "album_id", "not_a_column"));

        // The primary key is ordered first and the unknown name is dropped.
        assertEquals(List.of("album_id", "artist"), filterable);
    }

    @Test
    void shouldNotRequireAllowFilteringForAFullPrimaryKeyRestriction() {
        assertAll(
                () -> assertFalse(albums().requiresAllowFiltering(List.of())),
                () -> assertFalse(albums().requiresAllowFiltering(List.of("album_id"))),
                () -> assertFalse(tracks().requiresAllowFiltering(List.of("album_id"))),
                () -> assertFalse(tracks().requiresAllowFiltering(List.of("album_id", "track_no")))
        );
    }

    @Test
    void shouldRequireAllowFilteringWhenThePartitionKeyIsNotFullyRestricted() {
        // A clustering column alone cannot locate a partition, so this is a scan even though
        // "track_no" is part of the primary key.
        assertTrue(tracks().requiresAllowFiltering(List.of("track_no")));
    }

    @Test
    void shouldRequireAllowFilteringForNonKeyColumnsEvenAlongsideTheFullPartitionKey() {
        assertAll(
                () -> assertTrue(albums().requiresAllowFiltering(List.of("artist"))),
                () -> assertTrue(albums().requiresAllowFiltering(List.of("album_id", "artist"))),
                () -> assertTrue(tracks().requiresAllowFiltering(List.of("album_id", "title")))
        );
    }

    @Test
    void shouldRecogniseSetColumns() {
        assertAll(
                () -> assertTrue(albums().isSetColumn("tags")),
                () -> assertFalse(albums().isSetColumn("artist")),
                () -> assertFalse(albums().isSetColumn("not_a_column"))
        );
    }

    @Test
    void shouldAlwaysRequireAllowFilteringForASetMembershipFilter() {
        // CONTAINS cannot be served from the primary key, so it is a scan even alongside one.
        assertAll(
                () -> assertTrue(albums().requiresAllowFiltering(List.of("tags"))),
                () -> assertTrue(albums().requiresAllowFiltering(List.of("album_id", "tags")))
        );
    }

    @Test
    void shouldBindSetValuesFromTheBodyOnInsert() {
        var body = new JsonObject()
                .put("album_id", ALBUM_ID.toString())
                .put("title", "Kind of Blue")
                .put("artist", "Miles Davis")
                .put("tags", new JsonArray().add("jazz").add("modal"));

        var values = albums().generateValuesForInsert(body);

        assertEquals(Set.of("jazz", "modal"), values.get(3));
    }

    @Test
    void shouldRenderSetColumnsAsJsonArraysWhenNormalizingARow() {
        lenient().when(row.getColumnDefinitions()).thenReturn(columnDefinitions);
        lenient().when(columnDefinitions.contains(anyString())).thenReturn(true);
        when(row.getObject("album_id")).thenReturn(ALBUM_ID);
        when(row.getObject("title")).thenReturn("Kind of Blue");
        when(row.getObject("artist")).thenReturn("Miles Davis");
        when(row.getObject("tags")).thenReturn(new LinkedHashSet<>(List.of("jazz", "modal")));

        var normalized = albums().normalizeRow(row);

        assertEquals(new JsonArray().add("jazz").add("modal"), normalized.getJsonArray("tags"));
    }

    @Test
    void shouldParseOnlyRawValuesThatMatchKnownColumnsInColumnOrder() {
        var rawValues = new LinkedHashMap<String, String>();
        rawValues.put("artist", "Miles Davis");
        rawValues.put("album_id", ALBUM_ID.toString());
        rawValues.put("unknown", "ignored");

        var values = albums().parseRawValues(rawValues);

        assertAll(
                () -> assertEquals(2, values.size()),
                () -> assertEquals(ALBUM_ID, values.get(0)),
                () -> assertEquals("Miles Davis", values.get(1))
        );
    }

    @Test
    void shouldGenerateInsertValuesForEveryColumnCoercedToDriverTypes() {
        var body = new JsonObject()
                .put("album_id", ALBUM_ID.toString())
                .put("title", "Kind of Blue")
                .put("artist", "Miles Davis")
                .put("release_year", 1959)
                .put("in_print", true);

        var values = albums().generateValuesForInsert(body);

        assertAll(
                () -> assertEquals(6, values.size()),
                () -> assertEquals(ALBUM_ID, values.get(0)),
                () -> assertInstanceOf(UUID.class, values.get(0)),
                () -> assertEquals("Kind of Blue", values.get(1)),
                // "tags" is absent from the body, so it binds as null rather than an empty set.
                () -> assertNull(values.get(3)),
                () -> assertEquals(1959, values.get(4)),
                () -> assertEquals(true, values.get(5))
        );
    }

    @Test
    void shouldGenerateUpdateValuesWithNonKeyBodyValuesFollowedByPathPrimaryKey() {
        // The body carries a stale key that must be ignored; the key comes from the path.
        var body = new JsonObject()
                .put("album_id", "99999999-9999-9999-9999-999999999999")
                .put("track_no", 7)
                .put("title", "So What")
                .put("duration_ms", 562000);
        var pathParams = new LinkedHashMap<String, String>();
        pathParams.put("album_id", ALBUM_ID.toString());
        pathParams.put("track_no", "1");

        var values = tracks().generateValuesForUpdate(body, pathParams);

        assertAll(
                () -> assertEquals(4, values.size()),
                () -> assertEquals("So What", values.get(0)),
                () -> assertEquals(562000L, values.get(1)),
                () -> assertInstanceOf(Long.class, values.get(1)),
                () -> assertEquals(ALBUM_ID, values.get(2)),
                () -> assertEquals(1, values.get(3))
        );
    }

    @Test
    void shouldReadPrimaryKeyValuesFromThePathInColumnOrder() {
        var pathParams = new LinkedHashMap<String, String>();
        pathParams.put("track_no", "1");
        pathParams.put("album_id", ALBUM_ID.toString());

        var values = tracks().primaryKeyValues(pathParams);

        assertAll(
                () -> assertEquals(2, values.size()),
                () -> assertEquals(ALBUM_ID, values.get(0)),
                () -> assertEquals(1, values.get(1))
        );
    }

    @Test
    void shouldNormalizeRowsSkippingNullsAndRenderingDriverTypesAsStrings() {
        lenient().when(row.getColumnDefinitions()).thenReturn(columnDefinitions);
        lenient().when(columnDefinitions.contains(anyString())).thenReturn(true);
        when(row.getObject("album_id")).thenReturn(ALBUM_ID);
        when(row.getObject("title")).thenReturn("Kind of Blue");
        when(row.getObject("artist")).thenReturn("Miles Davis");
        when(row.getObject("release_year")).thenReturn(1959);
        when(row.getObject("in_print")).thenReturn(null);
        // Cassandra stores an empty collection as null, so a set column reads back as null too.
        when(row.getObject("tags")).thenReturn(null);

        var normalized = albums().normalizeRow(row);

        assertAll(
                () -> assertFalse(normalized.containsKey("tags")),
                () -> assertEquals(ALBUM_ID.toString(), normalized.getString("album_id")),
                () -> assertEquals("Kind of Blue", normalized.getString("title")),
                () -> assertEquals(1959, normalized.getInteger("release_year")),
                () -> assertFalse(normalized.containsKey("in_print"))
        );
    }

    @Test
    void shouldSkipColumnsMissingFromAProjection() {
        // A "SELECT <primary key>" projection does not carry every column of the table.
        when(row.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.contains(anyString())).thenReturn(false);
        when(columnDefinitions.contains("album_id")).thenReturn(true);
        when(row.getObject("album_id")).thenReturn(ALBUM_ID);

        var normalized = albums().normalizeRow(row);

        assertAll(
                () -> assertEquals(1, normalized.size()),
                () -> assertEquals(ALBUM_ID.toString(), normalized.getString("album_id"))
        );
    }

    private CassandraTableMetadata albums() {
        var tableMetadata = new TableMetadata("music", "music", "albums", List.of(
                column("albums", "album_id", "uuid", true),
                column("albums", "title", "text", false),
                column("albums", "artist", "text", false),
                column("albums", "tags", "set<text>", false),
                column("albums", "release_year", "int", false),
                column("albums", "in_print", "boolean", false)
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
