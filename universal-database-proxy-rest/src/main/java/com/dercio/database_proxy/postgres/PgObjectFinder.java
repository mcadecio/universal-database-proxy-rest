package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.TableMetadata;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.dercio.database_proxy.postgres.PgTypeMapper.INTEGER;
import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PgObjectFinder {
    private final SqlClient sqlClient;

    public Future<List<JsonObject>> find(TableMetadata tableMetadata) {

        String query = format("SELECT * FROM %s.%s", tableMetadata.getSchemaName(), tableMetadata.getTableName());

        return sqlClient.query(query)
                .execute()
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .collect(Collectors.toList()))
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()));
    }

    public Future<Optional<JsonObject>> findById(TableMetadata tableMetadata, Map<String, String> pathParams) {

        return sqlClient.preparedQuery(generateSelectQuery(tableMetadata))
                .execute(Tuple.of(findPkValue(tableMetadata, pathParams)))
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .findFirst());
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

}
