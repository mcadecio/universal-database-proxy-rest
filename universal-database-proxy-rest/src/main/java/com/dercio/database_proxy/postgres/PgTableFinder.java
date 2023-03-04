package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.database.TableRequest;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PgTableFinder {

    private static final String RETRIEVE_ALL_NON_DEFAULT_TABLES_FOR_DB =
            "WITH \"constraint\" AS (SELECT table_catalog, table_schema, table_name, constraint_name " +
                    "                      FROM information_schema.table_constraints " +
                    "                      WHERE constraint_type = 'PRIMARY KEY'), " +
                    "     pk_columns AS (SELECT kcu.table_catalog, kcu.table_schema, kcu.table_name, column_name " +
                    "                    FROM information_schema.key_column_usage kcu " +
                    "                             INNER JOIN \"constraint\" con on con.table_name = kcu.table_name " +
                    "                        AND con.table_schema = kcu.table_schema " +
                    "                        AND con.constraint_name = kcu.constraint_name " +
                    "                    WHERE con.table_schema NOT IN ('pg_catalog', 'information_schema', 'crdb_internal', 'pg_extension')) " +
                    "SELECT columns.table_catalog, " +
                    "       columns.table_schema, " +
                    "       columns.table_name, " +
                    "       columns.column_name, " +
                    "       ordinal_position, " +
                    "       column_default, " +
                    "       is_nullable, " +
                    "       data_type, " +
                    "       character_maximum_length, " +
                    "       exists(SELECT 1 WHERE columns.column_name = pk_columns.column_name) AS is_primary_key " +
                    "FROM information_schema.columns " +
                    "         FULL OUTER JOIN pk_columns ON " +
                    "            columns.table_catalog = pk_columns.table_catalog AND " +
                    "            columns.table_schema = pk_columns.table_schema AND " +
                    "            columns.table_name = pk_columns.table_name " +
                    "WHERE columns.table_catalog = $1 " +
                    "AND columns.table_schema NOT IN ('pg_catalog', 'information_schema', 'crdb_internal', 'pg_extension')";

    private final Map<String, TableMetadata> tableInfoCache = new ConcurrentHashMap<>();
    private final SqlClient sqlClient;

    public Future<List<TableMetadata>> findTables(String database) {

        return sqlClient.preparedQuery(RETRIEVE_ALL_NON_DEFAULT_TABLES_FOR_DB)
                .execute(Tuple.of(database))
                .map(this::mapRowsToColumnMetadata)
                .map(columns -> createTablesFromColumns(database, columns))
                .onSuccess(tableInfos -> log.debug("Found [{}] tables for [{}] database", tableInfos.size(), database))
                .onSuccess(tableInfos -> tableInfos.forEach(tableMetadata -> tableInfoCache.put(tableMetadata.getTableName(), tableMetadata)));
    }

    public Future<TableMetadata> findTable(TableRequest tableOption) {
        var database = tableOption.getDatabase();
        var schema = tableOption.getSchema();
        var table = tableOption.getTable();
        log.info("Retrieving table schema for {} | {} | {} ", database, schema, table);

        if (tableInfoCache.containsKey(table)) {
            log.info("Table Info already present in cache");
            return Future.succeededFuture(tableInfoCache.get(table));
        }

        return findTables(database)
                .map(tables -> tables.stream()
                        .filter(tableMetadata -> schema.equals(tableMetadata.getSchemaName()))
                        .filter(tableMetadata -> table.equals(tableMetadata.getTableName()))
                        .findFirst()
                        .orElseThrow(() -> new PgException("Table requested does not exist", null, null, null)));
    }

    private List<ColumnMetadata> mapRowsToColumnMetadata(RowSet<Row> rows) {
        return StreamSupport.stream(rows.spliterator(), false)
                .map(Row::toJson)
                .sorted(Comparator.comparing(json -> json.getInteger("ordinal_position")))
                .map(ColumnMetadata::new)
                .collect(Collectors.toList());
    }

    private List<TableMetadata> createTablesFromColumns(String database, List<ColumnMetadata> mixedColumns) {

        return groupColumnsBySchemaAndTableName(mixedColumns)
                .entrySet()
                .stream()
                .map(schemaTablesEntry -> {
                    String schemaName = schemaTablesEntry.getKey();
                    Map<String, List<ColumnMetadata>> rawTables = schemaTablesEntry.getValue();
                    return createTablesFromSchema(database, schemaName, rawTables);
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<TableMetadata> createTablesFromSchema(
            String database,
            String schemaName,
            Map<String, List<ColumnMetadata>> rawTables
    ) {
        return rawTables.entrySet()
                .stream()
                .map(rawTableEntry -> {
                    String tableName = rawTableEntry.getKey();
                    List<ColumnMetadata> columns = rawTableEntry.getValue();

                    return createTableMetadata(database, schemaName, tableName, columns);
                })
                .collect(Collectors.toList());
    }

    private Map<String, Map<String, List<ColumnMetadata>>> groupColumnsBySchemaAndTableName(
            List<ColumnMetadata> mixedColumns
    ) {
        return mixedColumns.stream().collect(Collectors.groupingBy(
                ColumnMetadata::getTableSchema,
                Collectors.groupingBy(ColumnMetadata::getTableName)
        ));
    }

    private TableMetadata createTableMetadata(String database, String schemaName, String tableName, List<ColumnMetadata> columns) {

        var tableMetadata = new TableMetadata(database, schemaName, tableName, columns);

        log.debug("Found table [{}] with [{}] columns named {}",
                tableMetadata.getTableName(),
                tableMetadata.getNumberOfColumns(),
                tableMetadata.getColumnNames());

        return tableMetadata;
    }

}
