package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.postgres.type.PgType;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class PgTableMetadata {
    private final TableMetadata tableMetadata;

    public String getDatabaseName() {
        return tableMetadata.getDatabaseName();
    }

    public String getSchemaName() {
        return tableMetadata.getSchemaName();
    }

    public String getTableName() {
        return tableMetadata.getTableName();
    }

    public List<ColumnMetadata> getColumns() {
        return tableMetadata.getColumns();
    }

    public List<String> getColumnNames() {
        return tableMetadata.getColumnNames();
    }

    public List<ColumnMetadata> getNonPrimaryKeyColumns() {
        return tableMetadata.getNonPrimaryKeyColumns();
    }

    public String getPkColumnName() {
        return tableMetadata.getPkColumnName();
    }

    public int getNumberOfColumns() {
        return tableMetadata.getNumberOfColumns();
    }

    public List<ColumnMetadata> getPrimaryKeyColumns() {
        return tableMetadata.getPrimaryKeyColumns();
    }

    public List<String> getPrimaryKeyColumnNames() {
        return tableMetadata.getPrimaryKeyColumnNames();
    }

    public String getQualifiedTableName() {
        return tableMetadata.getSchemaName() + "." + tableMetadata.getTableName();
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

}
