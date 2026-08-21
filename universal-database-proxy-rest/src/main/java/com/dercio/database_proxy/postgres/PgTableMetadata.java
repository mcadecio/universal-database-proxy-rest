package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.postgres.type.PgType;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class PgTableMetadata {
    private final TableMetadata tableMetadata;

    public String getTableName() {
        return tableMetadata.getTableName();
    }

    public List<ColumnMetadata> getColumns() {
        return tableMetadata.getColumns();
    }

    public String getQualifiedTableName() {
        return tableMetadata.getSchemaName() + "." + tableMetadata.getTableName();
    }

    public List<ColumnMetadata> getPrimaryKeyColumns() {
        return tableMetadata.getPrimaryKeyColumns();
    }

    public List<ColumnMetadata> getNonPrimaryKeyColumns() {
        return tableMetadata.getNonPrimaryKeyColumns();
    }

    public Tuple parseRawValues(Map<String, String> rawValues) {
        if (rawValues.isEmpty()) {
            return Tuple.tuple();
        }

        List<Object> values = getColumns()
                .stream()
                .filter(column -> rawValues.containsKey(column.getColumnName()))
                .map(column -> {
                    var columnName = column.getColumnName();
                    var value = rawValues.get(columnName);
                    return PgType.parse(column.getDbType(), value);
                })
                .toList();

        return Tuple.from(values);
    }

    public List<JsonObject> normalizeRows(RowSet<Row> rows) {
        return StreamSupport.stream(rows.spliterator(), false)
                .map(Row::toJson)
                .map(this::normalizeRow)
                .toList();
    }

    public JsonObject normalizeRow(JsonObject row) {
        JsonObject normalizedRow = new JsonObject();
        List<ColumnMetadata> columns = getColumns();
        for (ColumnMetadata column : columns) {
            String columnName = column.getColumnName();
            Object value = row.getValue(columnName);
            if (value != null) {
                normalizedRow.put(columnName, PgType.toRowValue(column.getDbType(), value));
            }
        }

        return normalizedRow;
    }

    public Tuple generateTupleForInsert(JsonObject body) {
        Tuple tuple = Tuple.tuple();
        for (ColumnMetadata column : getColumns()) {
            Object value = body.getValue(column.getColumnName());
            tuple.addValue(PgType.toSqlValue(column.getDbType(), value));
        }
        return tuple;
    }

    public Tuple generateTupleForUpdate(JsonObject body, Map<String, String> pathParameters) {
        Tuple tuple = Tuple.tuple();

        // SET only the non-primary-key columns so an update never overwrites the PK ...
        for (ColumnMetadata column : getNonPrimaryKeyColumns()) {
            Object value = body.getValue(column.getColumnName());
            tuple.addValue(PgType.toSqlValue(column.getDbType(), value));
        }

        // ... then bind the primary key values (from the path) used by the WHERE clause.
        for (ColumnMetadata column : getPrimaryKeyColumns()) {
            String columnName = column.getColumnName();
            if (pathParameters.containsKey(columnName)) {
                Object parsedValue = PgType.parse(column.getDbType(), pathParameters.get(columnName));
                tuple.addValue(parsedValue);
            }
        }

        return tuple;
    }
}
