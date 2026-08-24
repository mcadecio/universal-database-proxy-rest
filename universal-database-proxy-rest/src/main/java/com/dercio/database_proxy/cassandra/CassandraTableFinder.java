package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.dercio.database_proxy.cassandra.type.CassandraType;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.database.TableRequest;
import com.dercio.database_proxy.postgres.InconsistentStateException;
import com.google.inject.Inject;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CassandraTableFinder {

    private static final String PARTITION_KEY = "partition_key";
    private static final String CLUSTERING = "clustering";

    // keyspace_name is the partition key of both system_schema tables, so these are single-partition
    // reads and need no ALLOW FILTERING.
    private static final String RETRIEVE_TABLES_FOR_KEYSPACE =
            "SELECT table_name FROM system_schema.tables WHERE keyspace_name = ?";

    private static final String RETRIEVE_COLUMNS_FOR_KEYSPACE =
            "SELECT table_name, column_name, type, kind, position FROM system_schema.columns WHERE keyspace_name = ?";

    private final Map<String, CassandraTableMetadata> tableInfoCache = new ConcurrentHashMap<>();
    private final CassandraClient cassandraClient;

    public Future<List<CassandraTableMetadata>> findTables(String keyspace) {
        return findBaseTableNames(keyspace)
                .compose(baseTables -> findColumns(keyspace, baseTables))
                .map(columnsByTable -> createTables(keyspace, columnsByTable))
                .onSuccess(tables -> log.debug("Found [{}] tables for [{}] keyspace", tables.size(), keyspace))
                .onSuccess(tables -> tables.forEach(table -> tableInfoCache.put(table.getTableName(), table)));
    }

    public Future<CassandraTableMetadata> findTable(TableRequest tableOption) {
        var keyspace = tableOption.getDatabase();
        var table = tableOption.getTable();
        log.info("Retrieving table schema for {} | {} ", keyspace, table);

        if (tableInfoCache.containsKey(table)) {
            log.info("Table Info already present in cache");
            return Future.succeededFuture(tableInfoCache.get(table));
        }

        return findTables(keyspace)
                .map(tables -> tables.stream()
                        .filter(tableMetadata -> table.equals(tableMetadata.getTableName()))
                        .findFirst()
                        .orElseThrow(() -> new InconsistentStateException("Table requested does not exist")));
    }

    /**
     * {@code system_schema.columns} also describes materialized views. Only base tables are exposed,
     * because a view cannot be written through.
     */
    private Future<Set<String>> findBaseTableNames(String keyspace) {
        return cassandraClient
                .executeWithFullFetch(SimpleStatement.newInstance(RETRIEVE_TABLES_FOR_KEYSPACE, keyspace))
                .map(rows -> rows.stream()
                        .map(row -> row.getString("table_name"))
                        .collect(Collectors.toSet()));
    }

    private Future<Map<String, List<JsonObject>>> findColumns(String keyspace, Set<String> baseTables) {
        return cassandraClient
                .executeWithFullFetch(SimpleStatement.newInstance(RETRIEVE_COLUMNS_FOR_KEYSPACE, keyspace))
                .map(rows -> rows.stream()
                        .filter(row -> baseTables.contains(row.getString("table_name")))
                        .map(row -> toColumnJson(keyspace, row))
                        .collect(Collectors.groupingBy(json -> json.getString("table_name"))));
    }

    private JsonObject toColumnJson(String keyspace, Row row) {
        var kind = row.getString("kind");
        var isPrimaryKey = PARTITION_KEY.equals(kind) || CLUSTERING.equals(kind);

        return new JsonObject()
                .put("table_schema", keyspace)
                .put("table_name", row.getString("table_name"))
                .put("column_name", row.getString("column_name"))
                .put("data_type", row.getString("type"))
                .put("is_nullable", isPrimaryKey ? "NO" : "YES")
                .put("is_primary_key", isPrimaryKey)
                .put("kind", kind)
                .put("position", row.getInt("position"));
    }

    private List<CassandraTableMetadata> createTables(String keyspace, Map<String, List<JsonObject>> columnsByTable) {
        return columnsByTable.entrySet()
                .stream()
                .map(entry -> createTableMetadata(keyspace, entry.getKey(), entry.getValue()))
                .toList();
    }

    private CassandraTableMetadata createTableMetadata(String keyspace, String tableName, List<JsonObject> rawColumns) {
        var orderedColumns = rawColumns.stream()
                .sorted(columnOrder())
                .toList();

        var columns = orderedColumns.stream()
                .map(json -> new ColumnMetadata(json, CassandraType::toOpenApiColumnType))
                .toList();

        var tableMetadata = new TableMetadata(keyspace, keyspace, tableName, columns);

        log.debug("Found table [{}] with [{}] columns named {}",
                tableMetadata.getTableName(),
                tableMetadata.getNumberOfColumns(),
                tableMetadata.getColumnNames());

        return new CassandraTableMetadata(
                tableMetadata,
                columnNamesOfKind(orderedColumns, PARTITION_KEY),
                columnNamesOfKind(orderedColumns, CLUSTERING)
        );
    }

    /**
     * Partition key columns first, then clustering columns, both in their declared {@code position}
     * order, then everything else alphabetically. This order becomes the composite-key path parameter
     * order in the generated OpenAPI, so it has to match the table's real key order.
     */
    private Comparator<JsonObject> columnOrder() {
        return Comparator.<JsonObject>comparingInt(json -> switch (json.getString("kind")) {
                    case PARTITION_KEY -> 0;
                    case CLUSTERING -> 1;
                    default -> 2;
                })
                .thenComparingInt(json -> json.getInteger("position", -1))
                .thenComparing(json -> json.getString("column_name"));
    }

    private List<String> columnNamesOfKind(List<JsonObject> orderedColumns, String kind) {
        return orderedColumns.stream()
                .filter(json -> kind.equals(json.getString("kind")))
                .map(json -> json.getString("column_name"))
                .toList();
    }
}
