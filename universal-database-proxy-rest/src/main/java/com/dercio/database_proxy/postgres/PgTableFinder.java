package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.database.TableRequest;
import com.google.inject.Inject;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
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
    private static final String RETRIEVE_PRIMARY_COLUMN_NAME_QUERY = "WITH \"constraint\" AS (SELECT constraint_name " +
            "                    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS\n" +
            "                    WHERE table_catalog = $1 " +
            "                      AND table_schema = $2 " +
            "                      AND table_name = $3 " +
            "                      AND constraint_type = 'PRIMARY KEY')" +
            "SELECT column_name " +
            "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu, \"constraint\" " +
            "WHERE kcu.constraint_name = \"constraint\".constraint_name" +
            " AND table_catalog = $1 " +
            " AND table_schema = $2 " +
            " AND table_name = $3 ";

    private static final String RETRIEVE_TABLE_SCHEMA_QUERY =
            "SELECT column_name," +
                    "   data_type, " +
                    "   character_maximum_length, " +
                    "   column_default, " +
                    "   is_nullable" +
                    " FROM INFORMATION_SCHEMA.COLUMNS" +
                    " WHERE table_catalog = $1 " +
                    "   AND table_schema = $2 " +
                    "   AND table_name = $3";

    private static final String RETRIEVE_ALL_NON_DEFAULT_TABLES_FOR_DB =
            "SELECT table_catalog, " +
                    "       table_schema, " +
                    "       table_name," +
                    "       column_name, " +
                    "       data_type, " +
                    "       character_maximum_length, " +
                    "       column_default, " +
                    "       is_nullable, " +
                    "       ordinal_position " +
                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE table_catalog = $1 " +
                    "  AND NOT (table_schema IN ('pg_catalog', 'information_schema', 'crdb_internal', 'pg_extension'))";

    private final Map<String, TableMetadata> tableInfoCache = new ConcurrentHashMap<>();
    private final SqlClient sqlClient;

    public Future<List<TableMetadata>> findTables(String database) {
        var collectBySchemaAndTableName = Collectors.groupingBy(
                ColumnMetadata::getTableSchema,
                Collectors.groupingBy(ColumnMetadata::getTableName)
        );

        return sqlClient.preparedQuery(RETRIEVE_ALL_NON_DEFAULT_TABLES_FOR_DB)
                .execute(Tuple.of(database))
                .compose(rows -> CompositeFuture.all(StreamSupport.stream(rows.spliterator(), false)
                                .sorted(Comparator.comparing(row -> row.getLong("ordinal_position")))
                                .map(Row::toJson)
                                .map(ColumnMetadata::new)
                                .collect(collectBySchemaAndTableName)
                                .entrySet()
                                .stream()
                                .flatMap(schemaEntry -> schemaEntry.getValue()
                                        .entrySet()
                                        .stream()
                                        .map(tableEntry -> {
                                            var tableMetadata = new TableMetadata(
                                                    tableEntry.getValue(),
                                                    database,
                                                    schemaEntry.getKey(),
                                                    tableEntry.getKey()
                                            );

                                            log.info("Found table [{}] with [{}] columns named {}",
                                                    tableMetadata.getTableName(),
                                                    tableMetadata.getNumberOfColumns(),
                                                    tableMetadata.getColumnNames());

                                            return tableMetadata;
                                        }))
                                .map(tableMetadata -> findPrimaryKeyColumn(tableMetadata)
                                        .onSuccess(tableMetadata::setPrimaryKeyColumn)
                                        .map(tableMetadata))
                                .collect(Collectors.toList()))
                        .map(CompositeFuture::<TableMetadata>list)
                )
                .onSuccess(tableInfos -> log.info("Found [{}] tables for [{}] database", tableInfos.size(), database))
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

        return sqlClient.preparedQuery(RETRIEVE_TABLE_SCHEMA_QUERY)
                .execute(Tuple.of(database, schema, table))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .map(ColumnMetadata::new)
                        .collect(Collectors.toList()))
                .map(columns -> new TableMetadata(columns, database, schema, table))
                .compose(tableMetadata -> findPrimaryKeyColumn(tableMetadata)
                        .onSuccess(tableMetadata::setPrimaryKeyColumn)
                        .map(tableMetadata))
                .onSuccess(tableMetadataInfo -> log.info("Successfully retrieved table schema"))
                .onSuccess(tableMetadataInfo -> tableInfoCache.put(tableMetadataInfo.getTableName(), tableMetadataInfo));
    }


    private Future<String> findPrimaryKeyColumn(TableMetadata tableMetadata) {
        var database = tableMetadata.getDatabaseName();
        var schema = tableMetadata.getSchemaName();
        var table = tableMetadata.getTableName();

        return sqlClient.preparedQuery(RETRIEVE_PRIMARY_COLUMN_NAME_QUERY)
                .execute(Tuple.of(database, schema, table))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(row -> row.getString("column_name"))
                        .findFirst()
                        .orElseGet(() -> {
                            var columnName = tableMetadata.getColumns().get(0).getColumnName();
                            log.info("Table [{}] does not have a PK. Using [{}] column as PK",
                                    tableMetadata.getTableName(),
                                    columnName
                            );
                            return columnName;
                        }));
    }
}
