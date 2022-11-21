package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableRequest;
import com.google.inject.Inject;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static com.dercio.database_proxy.postgres.PgTypeMapper.INTEGER;
import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PgRepository implements Repository {

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
                    "       ordinal_position " + // TODO: See if you can stop relying on this
                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE table_catalog = $1 " +
                    "  AND NOT (table_schema IN ('pg_catalog', 'information_schema', 'crdb_internal', 'pg_extension'))";

    private final Map<String, TableMetadata> tableInfoCache = new ConcurrentHashMap<>();
    private final SqlClient sqlClient;

    @Override
    public Future<List<TableMetadata>> getTables(String database) {
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
                                                    tableMetadata.getColumns().size(),
                                                    tableMetadata.getColumnNames());

                                            return tableMetadata;
                                        }))
                                .map(tableMetadata -> findPrimaryKeyColumn(tableMetadata)
                                        .onSuccess(tableMetadata::setPkColumnName)
                                        .map(tableMetadata))
                                .collect(Collectors.toList()))
                        .map(CompositeFuture::<TableMetadata>list)
                )
                .onSuccess(tableInfos -> log.info("Found [{}] tables for [{}] database", tableInfos.size(), database))
                .onSuccess(tableInfos -> tableInfos.forEach(tableMetadata -> tableInfoCache.put(tableMetadata.getTableName(), tableMetadata)));
    }

    @Override
    public Future<TableMetadata> getTableInfo(TableRequest tableOption) {
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
                        .onSuccess(tableMetadata::setPkColumnName)
                        .map(tableMetadata))
                .onSuccess(tableMetadataInfo -> log.info("Successfully retrieved table schema"))
                .onSuccess(tableMetadataInfo -> tableInfoCache.put(tableMetadataInfo.getTableName(), tableMetadataInfo));
    }

    @Override
    public Future<List<JsonObject>> getData(TableRequest tableOption) {
        log.info("Retrieving rows for {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        String query = format("SELECT * FROM %s.%s", tableOption.getSchema(), tableOption.getTable());

        return sqlClient.query(query)
                .execute()
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .collect(Collectors.toList()))
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()));
    }

    @Override
    public Future<Optional<JsonObject>> getDataById(TableRequest tableOption, Map<String, String> pathParams) {
        log.info("Retrieving row for {} | {} | {}",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableMetadata -> sqlClient.preparedQuery(generateSelectQuery(tableMetadata))
                        .execute(Tuple.of(findPkValue(tableMetadata, pathParams))))
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .findFirst());
    }

    @Override
    public Future<Object> createData(TableRequest tableOption, JsonObject data) {
        log.info("Inserting data into {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableMetadata -> sqlClient.preparedQuery(generateInsertQuery(tableMetadata))
                        .execute(generateTupleForInsert(tableMetadata, data)))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(row -> row.getValue(0))
                        .findFirst()
                        .orElseThrow());
    }

    @Override
    public Future<Integer> updateData(
            TableRequest tableOption,
            JsonObject data,
            Map<String, String> pathParams) {

        log.info("Updating data in {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableMetadata -> {
                    if (tableMetadata.getColumns().size() == 1) {
                        return Future.failedFuture(new IllegalStateException("Unable to update table with only one column"));
                    }
                    return Future.succeededFuture(tableMetadata);
                })
                .compose(tableMetadataInfo -> validaUpdateRequest(tableMetadataInfo, data, pathParams))
                .compose(tableMetadata -> sqlClient.preparedQuery(generateUpdateQuery(tableMetadata))
                        .execute(generateTupleForInsert(tableMetadata, data)))
                .map(SqlResult::rowCount)
                .onSuccess(count -> log.info("Rows updated [{}]", count));
    }

    @Override
    public Future<Integer> deleteData(TableRequest tableOption, Map<String, String> pathParams) {
        log.info("Deleting item from {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableMetadata -> sqlClient.preparedQuery(generateDeleteQuery(tableMetadata))
                        .execute(Tuple.of(findPkValue(tableMetadata, pathParams))))
                .map(SqlResult::rowCount)
                .onSuccess(count -> log.info("Rows deleted [{}]", count));
    }

    private Future<TableMetadata> validaUpdateRequest(TableMetadata tableMetadata, JsonObject data, Map<String, String> pathParams) {
        var resourceIdInPath = pathParams.get(tableMetadata.getPkColumnName());
        var resourceIdInBody = data.getString(tableMetadata.getPkColumnName());

        if (resourceIdInBody.equals(resourceIdInPath)) {
            return Future.succeededFuture(tableMetadata);
        }

        return Future.failedFuture(new InconsistentStateException());
    }

    private Object findPkValue(TableMetadata tableMetadata, Map<String, String> pathParams) {
        var primaryKeyColumn = tableMetadata.getPrimaryKeyColumn();
        var value = pathParams.get(primaryKeyColumn.getColumnName());
        return primaryKeyColumn.getOpenApiType().equals(INTEGER) ? Long.parseLong(value) : value;
    }

    private String generateSelectQuery(TableMetadata tableMetadata) {

        var query = format(
                "SELECT * FROM %s.%s WHERE %s = $1",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                tableMetadata.getPkColumnName()
        );

        log.info("Generated select query [{}]", query);

        return query;
    }

    private String generateDeleteQuery(TableMetadata tableMetadata) {

        var query = format(
                "DELETE FROM %s.%s WHERE %s = $1 RETURNING %s",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                tableMetadata.getPkColumnName(),
                tableMetadata.getPkColumnName()
        );

        log.info("Generated delete query [{}]", query);

        return query;
    }

    private String generateInsertQuery(TableMetadata tableMetadata) {
        var baseQuery = format(
                "INSERT INTO %s.%s(%s) VALUES ",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                String.join(", ", tableMetadata.getColumnNames())
        );

        String valuePlaceholders = "(" +
                IntStream.rangeClosed(1, tableMetadata.getColumns().size())
                        .mapToObj(i -> String.format("$%d", i))
                        .collect(Collectors.joining(",")) +
                ") RETURNING " + tableMetadata.getPkColumnName();

        var finalQuery = baseQuery + valuePlaceholders;

        log.info("Generated insert query {}", finalQuery);

        return finalQuery;
    }

    private Tuple generateTupleForInsert(TableMetadata tableMetadata, JsonObject body) {
        return Tuple.from(tableMetadata.getColumns()
                .stream()
                .map(column -> {

                    var columnName = column.getColumnName();

                    if (column.getDbType().equals("timestamp without time zone")) {
                        var rawTimestamp = body.getString(columnName);
                        return rawTimestamp == null ? null : LocalDateTime.parse(rawTimestamp);
                    } else if (column.getDbType().equals("timestamp with time zone")) {
                        var rawTimestamp = body.getString(columnName);
                        return rawTimestamp == null ? null : OffsetDateTime.parse(rawTimestamp);
                    }

                    return body.getValue(columnName);
                })
                .collect(Collectors.toList()));
    }

    private String generateColumnsToUpdate(List<ColumnMetadata> columns) {
        return IntStream.range(0, columns.size())
                .mapToObj(i -> format("%s = $%d",
                        columns.get(i).getColumnName(),
                        i + 2))
                .collect(Collectors.joining(", "));
    }

    private String generateUpdateQuery(TableMetadata tableMetadata) {

        var values = generateColumnsToUpdate(tableMetadata.getNonPrimaryKeyColumns());

        var query = format(
                "UPDATE %s.%s SET %s WHERE %s = $1 RETURNING %s",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                values,
                tableMetadata.getPkColumnName(),
                tableMetadata.getPkColumnName()
        );

        log.info("Generated update query {}", query);

        return query;
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
