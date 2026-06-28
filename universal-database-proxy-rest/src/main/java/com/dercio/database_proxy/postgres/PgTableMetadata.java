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

    public Tuple generateTupleForWrite(JsonObject body, Map<String, String> pathParameters) {
        Tuple tuple = Tuple.tuple();
        List<ColumnMetadata> columns = getColumns();
        for (ColumnMetadata column: columns) {
            String columnName = column.getColumnName();
            Object value = body.getValue(columnName);
            Object sqlValue = PgType.toSqlValue(column.getDbType(), value);
            tuple.addValue(sqlValue);
        }

        List<ColumnMetadata> primaryKeyColumns = getPrimaryKeyColumns();
        for (ColumnMetadata column: primaryKeyColumns) {
            String columnName = column.getColumnName();
            if (pathParameters.containsKey(columnName)) {
                String value = pathParameters.get(columnName);
                Object parsedValue = PgType.parse(column.getDbType(), value);
                tuple.addValue(parsedValue);
            }
        }

        return tuple;
    }
}
