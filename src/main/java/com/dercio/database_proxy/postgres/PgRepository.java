package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.database.Table;
import com.dercio.database_proxy.common.database.TableColumn;
import com.dercio.database_proxy.common.database.TableRequest;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.dercio.database_proxy.postgres.PgTypeMapper.STRING;
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
            "WHERE kcu.constraint_name = \"constraint\".constraint_name";
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

    private final Map<String, Table> tableInfoCache = new ConcurrentHashMap<>();
    private final Map<String, SqlClient> sqlClientMap;


    public Future<Table> getTableInfo(TableRequest tableOption) {
        var database = tableOption.getDatabase();
        var schema = tableOption.getSchema();
        var table = tableOption.getTable();
        var client = sqlClientMap.get(tableOption.getDatabase());
        log.info("Retrieving table schema for {} | {} | {} ", database, schema, table);

        if (tableInfoCache.containsKey(table)) {
            log.info("Table Info already present in cache");
            return Future.succeededFuture(tableInfoCache.get(table));
        }

        return client.preparedQuery(RETRIEVE_TABLE_SCHEMA_QUERY)
                .execute(Tuple.of(database, schema, table))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .map(TableColumn::new)
                        .collect(Collectors.toList()))
                .map(columns -> new Table(columns, database, schema, table))
                .compose(tableInfo -> client.preparedQuery(RETRIEVE_PRIMARY_COLUMN_NAME_QUERY)
                        .execute(Tuple.of(database, schema, table))
                        .map(rows -> rows.iterator().next())
                        .map(row -> row.getString("column_name"))
                        .onSuccess(tableInfo::setPkColumnName)
                        .map(tableInfo))
                .onSuccess(tableInfo -> log.info("Successfully retrieved table schema"))
                .onSuccess(tableInfo -> tableInfoCache.put(tableInfo.getTableName(), tableInfo));
    }

    public Future<List<JsonObject>> getData(TableRequest tableOption) {
        log.info("Retrieving rows for {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        var client = sqlClientMap.get(tableOption.getDatabase());

        String query = format("SELECT * FROM %s.%s", tableOption.getSchema(), tableOption.getTable());

        return client.query(query)
                .execute()
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .collect(Collectors.toList()))
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()));
    }

    public Future<Optional<JsonObject>> getDataById(TableRequest tableOption, Map<String, String> pathParams) {
        log.info("Retrieving row for {} | {} | {}",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        var client = sqlClientMap.get(tableOption.getDatabase());

        return getTableInfo(tableOption)
                .map(tableInfo -> generateSelectQuery(tableInfo, pathParams))
                .compose(query -> client.query(query).execute())
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .findFirst());
    }

    public Future<Void> createData(TableRequest tableOption, JsonObject data) {
        var client = sqlClientMap.get(tableOption.getDatabase());

        log.info("Inserting data into {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .map(tableInfo -> generateInsertQuery(tableInfo, data))
                .compose(query -> client.query(query).execute())
                .mapEmpty();
    }

    public Future<Void> updateData(
            TableRequest tableOption,
            JsonObject data,
            Map<String, String> pathParams) {

        var client = sqlClientMap.get(tableOption.getDatabase());

        log.info("Updating data in {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableInfo -> validaUpdateRequest(tableInfo, data, pathParams))
                .map(tableInfo -> generateUpdateQuery(tableInfo, data))
                .compose(query -> client.query(query).execute())
                .mapEmpty();
    }

    public Future<Void> deleteData(TableRequest tableOption, Map<String, String> pathParams) {
        var client = sqlClientMap.get(tableOption.getDatabase());

        log.info("Deleting item from {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .map(tableInfo -> generateDeleteQuery(tableInfo, pathParams))
                .compose(query -> client.query(query).execute())
                .mapEmpty();
    }

    private Future<Table> validaUpdateRequest(Table table, JsonObject data, Map<String, String> pathParams) {
        var resourceIdInPath = pathParams.get(table.getPkColumnName());
        var resourceIdInBody = data.getString(table.getPkColumnName());

        if (resourceIdInBody.equals(resourceIdInPath)) {
            return Future.succeededFuture(table);
        }

        return Future.failedFuture(new IllegalStateException("Inconsistent Ids"));
    }

    private String generateSelectQuery(Table table, Map<String, String> pathParams) {

        var primaryKeyValue = table.getColumns()
                .stream()
                .filter(column -> column.getColumnName().equals(table.getPkColumnName()))
                .findFirst()
                .map(column -> {
                    var value = pathParams.get(column.getColumnName());
                    if (Objects.equals(column.getDataType(), STRING)) {
                        return "'" + value + "'";
                    }
                    return value;
                })
                .orElseThrow();

        var query = format(
                "SELECT * FROM %s.%s WHERE %s = %s",
                table.getSchemaName(),
                table.getTableName(),
                table.getPkColumnName(),
                primaryKeyValue
        );

        log.info("Generated select query [{}]", query);

        return query;
    }

    private String generateDeleteQuery(Table table, Map<String, String> pathParams) {
        var primaryKeyValue = table.getColumns()
                .stream()
                .filter(column -> column.getColumnName().equals(table.getPkColumnName()))
                .findFirst()
                .map(column -> {
                    var value = pathParams.get(column.getColumnName());
                    if (Objects.equals(column.getDataType(), STRING)) {
                        return "'" + value + "'";
                    }
                    return value;
                })
                .orElseThrow();

        var query = format(
                "DELETE FROM %s.%s WHERE %s = %s",
                table.getSchemaName(),
                table.getTableName(),
                table.getPkColumnName(),
                primaryKeyValue
        );

        log.info("Generated delete query [{}]", query);

        return query;
    }

    private String generateInsertQuery(Table table, JsonObject body) {
        StringBuilder builder = new StringBuilder();
        table.getColumns()
                .stream()
                .map(column -> {
                    var value = body.getValue(column.getColumnName());
                    if (Objects.equals(column.getDataType(), STRING)) {
                        return "'" + value + "'";
                    }
                    return value;
                })
                .forEach(value -> {
                    builder.append(value);
                    builder.append(",");
                });

        var values = builder.toString();

        var query = format(
                "INSERT INTO %s.%s(%s) VALUES (%s)",
                table.getSchemaName(),
                table.getTableName(),
                table.getColumns()
                        .stream()
                        .map(TableColumn::getColumnName)
                        .collect(Collectors.joining(", ")),
                values.substring(0, values.length() - 1)
        );

        log.info("Generated insert query {}", query);


        return query;
    }

    private String generateUpdateQuery(Table table, JsonObject body) {
        StringBuilder builder = new StringBuilder();
        table.getColumns()
                .stream()
                .filter(column -> !column.getColumnName().equals(table.getPkColumnName()))
                .map(column -> {
                    var value = body.getValue(column.getColumnName());
                    if (Objects.equals(column.getDataType(), STRING)) {
                        value = "'" + value + "'";
                    }

                    return format("%s = %s", column.getColumnName(), value);
                })
                .forEach(value -> {
                    builder.append(value);
                    builder.append(",");
                });

        var values = builder.toString();

        var primaryKeyValue = table.getColumns()
                .stream()
                .filter(column -> column.getColumnName().equals(table.getPkColumnName()))
                .findFirst()
                .map(column -> {
                    var value = body.getValue(column.getColumnName());
                    if (Objects.equals(column.getDataType(), STRING)) {
                        return "'" + value + "'";
                    }
                    return value;
                })
                .orElseThrow();

        var query = format(
                "UPDATE %s.%s SET %s WHERE %s = %s",
                table.getSchemaName(),
                table.getTableName(),
                values.substring(0, values.length() - 1),
                table.getPkColumnName(),
                primaryKeyValue
        );

        log.info("Generated update query {}", query);

        return query;
    }

}
