package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.dercio.database_proxy.cassandra.type.CassandraType;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class CassandraTableMetadata {
    @Getter
    private final TableMetadata tableMetadata;
    private final List<String> partitionKeyColumnNames;
    private final List<String> clusteringColumnNames;

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

    public List<String> filterableColumnNames(Collection<String> filterNames) {
        return getColumns()
                .stream()
                .map(ColumnMetadata::getColumnName)
                .filter(filterNames::contains)
                .toList();
    }

    public boolean isSetColumn(String columnName) {
        return getColumns()
                .stream()
                .filter(column -> column.getColumnName().equals(columnName))
                .anyMatch(column -> CassandraType.isSet(column.getDbType()));
    }

    public boolean requiresAllowFiltering(Collection<String> filterNames) {
        if (filterNames.isEmpty()) {
            return false;
        }

        if (filterNames.stream().anyMatch(this::isSetColumn)) {
            return true;
        }

        if (!filterNames.containsAll(partitionKeyColumnNames)) {
            return true;
        }

        var remaining = new HashSet<>(filterNames);
        partitionKeyColumnNames.forEach(remaining::remove);

        for (String clusteringColumn : clusteringColumnNames) {
            if (!remaining.remove(clusteringColumn)) {
                break;
            }
        }

        return !remaining.isEmpty();
    }

    public List<Object> parseRawValues(Map<String, String> rawValues) {
        return getColumns()
                .stream()
                .filter(column -> rawValues.containsKey(column.getColumnName()))
                .map(column -> CassandraType.parse(column.getDbType(), rawValues.get(column.getColumnName())))
                .toList();
    }

    public List<JsonObject> normalizeRows(List<Row> rows) {
        return rows.stream().map(this::normalizeRow).toList();
    }

    public JsonObject normalizeRow(Row row) {
        var normalizedRow = new JsonObject();

        for (ColumnMetadata column : getColumns()) {
            var columnName = column.getColumnName();

            if (!row.getColumnDefinitions().contains(columnName)) {
                continue;
            }

            var value = row.getObject(columnName);
            if (value != null) {
                normalizedRow.put(columnName, CassandraType.toRowValue(column.getDbType(), value));
            }
        }

        return normalizedRow;
    }

    public List<Object> generateValuesForInsert(JsonObject body) {
        return getColumns()
                .stream()
                .map(column -> CassandraType.toSqlValue(column.getDbType(), body.getValue(column.getColumnName())))
                .toList();
    }

    public List<Object> generateValuesForUpdate(JsonObject body, Map<String, String> pathParameters) {
        var values = new ArrayList<>(getNonPrimaryKeyColumns()
                .stream()
                .map(column -> CassandraType.toSqlValue(column.getDbType(), body.getValue(column.getColumnName())))
                .toList());

        values.addAll(primaryKeyValues(pathParameters));

        return values;
    }

    public List<Object> primaryKeyValues(Map<String, String> pathParameters) {
        return getPrimaryKeyColumns()
                .stream()
                .filter(column -> pathParameters.containsKey(column.getColumnName()))
                .map(column -> CassandraType.parse(column.getDbType(), pathParameters.get(column.getColumnName())))
                .toList();
    }

    public List<Object> primaryKeyValues(Row row) {
        return getPrimaryKeyColumns()
                .stream()
                .map(column -> row.getObject(column.getColumnName()))
                .toList();
    }
}
