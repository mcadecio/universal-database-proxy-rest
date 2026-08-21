package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Spliterators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgObjectInserterTest {

    @Mock
    private SqlClient sqlClient;
    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;
    @Mock
    private RowSet<Row> rowSet;

    @Test
    void updateShouldNotIncludeThePrimaryKeyInTheSetClauseAndBindsPkFromPath() {
        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        var tupleCaptor = ArgumentCaptor.forClass(Tuple.class);
        when(sqlClient.preparedQuery(sqlCaptor.capture())).thenReturn(preparedQuery);
        when(preparedQuery.execute(tupleCaptor.capture())).thenReturn(Future.succeededFuture(rowSet));
        when(rowSet.rowCount()).thenReturn(1);

        var inserter = new PgObjectInserter(sqlClient);
        // The body carries a stale primary key which must be ignored; the PK comes from the path.
        var body = new JsonObject()
                .put("car_id", 99)
                .put("manufacturer", "toyota")
                .put("doors", 4)
                .put("extra_details", new JsonObject().put("colour", "red"));

        inserter.update(carsTable(), body, Map.of("car_id", "7"));

        var setClause = sqlCaptor.getValue().split("SET")[1].split("WHERE")[0];
        var tuple = tupleCaptor.getValue();

        assertAll(
                () -> assertFalse(setClause.contains("car_id"), "SET clause must not touch the primary key"),
                () -> assertTrue(sqlCaptor.getValue().contains("WHERE car_id = ?")),
                () -> assertTrue(setClause.contains("extra_details = ?::jsonb")),
                // 4 non-pk columns + 1 pk bound from the path
                () -> assertEquals(5, tuple.size()),
                () -> assertEquals(7L, tuple.getValue(4))
        );
    }

    @Test
    void updateShouldStayValidForTablesWhereEveryColumnIsPartOfThePrimaryKey() {
        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        var tupleCaptor = ArgumentCaptor.forClass(Tuple.class);
        when(sqlClient.preparedQuery(sqlCaptor.capture())).thenReturn(preparedQuery);
        when(preparedQuery.execute(tupleCaptor.capture())).thenReturn(Future.succeededFuture(rowSet));
        when(rowSet.rowCount()).thenReturn(1);

        var wheelTable = new TableMetadata("defaultdb", "vehicles", "wheel", List.of(
                column("wheel_type", "character varying", true)
        ));
        var inserter = new PgObjectInserter(sqlClient);

        inserter.update(wheelTable, new JsonObject().put("wheel_type", "ALLOY"), Map.of("wheel_type", "STEEL"));

        assertAll(
                // A no-op self-assignment keeps the statement valid instead of "SET  WHERE ...".
                () -> assertEquals("UPDATE vehicles.wheel SET wheel_type = wheel_type WHERE wheel_type = ?",
                        sqlCaptor.getValue()),
                () -> assertEquals(1, tupleCaptor.getValue().size()),
                () -> assertEquals("STEEL", tupleCaptor.getValue().getValue(0))
        );
    }

    @Test
    void createShouldInsertEveryColumnAndReturnThePrimaryKey() {
        var sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(sqlClient.preparedQuery(sqlCaptor.capture())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));
        when(rowSet.spliterator()).thenReturn(Spliterators.emptySpliterator());

        var inserter = new PgObjectInserter(sqlClient);

        inserter.create(carsTable(), new JsonObject().put("car_id", 1).put("manufacturer", "toyota"));

        var sql = sqlCaptor.getValue();
        assertAll(
                () -> assertTrue(sql.startsWith(
                        "INSERT INTO vehicles.cars(car_id, manufacturer, doors, last_updated, extra_details) VALUES")),
                () -> assertTrue(sql.contains("?::jsonb")),
                () -> assertTrue(sql.endsWith("RETURNING car_id"))
        );
    }

    private TableMetadata carsTable() {
        return new TableMetadata("defaultdb", "vehicles", "cars", List.of(
                column("manufacturer", "character varying", false),
                column("car_id", "bigint", true),
                column("doors", "bigint", false),
                column("last_updated", "timestamp with time zone", false),
                column("extra_details", "jsonb", false)
        ));
    }

    private ColumnMetadata column(String name, String dataType, boolean primaryKey) {
        return new ColumnMetadata(new JsonObject()
                .put("table_schema", "vehicles")
                .put("table_name", "cars")
                .put("column_name", name)
                .put("data_type", dataType)
                .put("is_nullable", "YES")
                .put("is_primary_key", primaryKey));
    }
}
