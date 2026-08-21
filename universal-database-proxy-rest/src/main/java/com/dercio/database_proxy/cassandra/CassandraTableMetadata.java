package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.dercio.database_proxy.cassandra.type.CassandraType;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * Decorates {@link TableMetadata} with the Cassandra-specific knowledge the shared model does not
 * carry: which primary key columns are <b>partition</b> keys versus <b>clustering</b> keys. That
 * distinction is what decides whether a query needs {@code ALLOW FILTERING}, so it cannot be derived
 * from {@code isPrimaryKey} alone.
 */
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

    /** Cassandra has no schemas — the keyspace plays that role. */
    public String getQualifiedTableName() {
        return tableMetadata.getSchemaName() + "." + tableMetadata.getTableName();
    }

    public List<ColumnMetadata> getPrimaryKeyColumns() {
        return tableMetadata.getPrimaryKeyColumns();
    }

    public List<ColumnMetadata> getNonPrimaryKeyColumns() {
        return tableMetadata.getNonPrimaryKeyColumns();
    }

    /**
     * Intersects the requested filter names with the real columns of this table, preserving column
     * order. This is the whitelist that keeps caller-supplied names out of the generated CQL.
     */
    public List<String> filterableColumnNames(Collection<String> filterNames) {
        return getColumns()
                .stream()
                .map(ColumnMetadata::getColumnName)
                .filter(filterNames::contains)
                .toList();
    }

    /**
     * CQL serves a restriction without a scan only when every partition key column is constrained
     * and the constrained clustering columns form a contiguous prefix. Anything else — a partial
     * partition key, a skipped clustering column, or a non-key column — needs {@code ALLOW FILTERING}.
     */
    public boolean requiresAllowFiltering(Collection<String> filterNames) {
        if (filterNames.isEmpty()) {
            return false;
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

    /** Parses raw path/query string values into driver types, ordered to match the generated WHERE clause. */
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

            // A projection such as SELECT <pk columns> does not carry every column of the table.
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

    /**
     * SET values first (non-primary-key columns only, so an update never rewrites the key), then the
     * primary key values bound by the WHERE clause. The order is load-bearing — it has to line up
     * with the statement built by {@code CassandraObjectInserter}.
     */
    public List<Object> generateValuesForUpdate(JsonObject body, Map<String, String> pathParameters) {
        var values = new ArrayList<>(getNonPrimaryKeyColumns()
                .stream()
                .map(column -> CassandraType.toSqlValue(column.getDbType(), body.getValue(column.getColumnName())))
                .toList());

        values.addAll(primaryKeyValues(pathParameters));

        return values;
    }

    /** The full primary key, in column order, parsed from the request path. */
    public List<Object> primaryKeyValues(Map<String, String> pathParameters) {
        return getPrimaryKeyColumns()
                .stream()
                .filter(column -> pathParameters.containsKey(column.getColumnName()))
                .map(column -> CassandraType.parse(column.getDbType(), pathParameters.get(column.getColumnName())))
                .toList();
    }

    /** The full primary key of an already-materialised row, in column order. */
    public List<Object> primaryKeyValues(Row row) {
        return getPrimaryKeyColumns()
                .stream()
                .map(column -> row.getObject(column.getColumnName()))
                .toList();
    }
}
