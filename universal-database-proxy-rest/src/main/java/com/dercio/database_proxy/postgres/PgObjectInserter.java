package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.postgres.type.PgType;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PgObjectInserter {

    private final SqlClient sqlClient;

    public Future<Object> create(TableMetadata tableMetadata, JsonObject data) {

        return sqlClient.preparedQuery(generateInsertQuery(tableMetadata))
                .execute(generateTupleForInsert(tableMetadata, data))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(row -> row.getValue(0))
                        .findFirst()
                        .orElseThrow());
    }

    public Future<Integer> update(TableMetadata tableMetadata, JsonObject data, Map<String, String> pathParams) {

        return validaUpdateRequest(tableMetadata, data, pathParams)
                .compose(empty -> sqlClient.preparedQuery(generateUpdateQuery(tableMetadata))
                        .execute(generateTupleForInsert(tableMetadata, data)))
                .map(SqlResult::rowCount)
                .onSuccess(count -> log.info("Rows updated [{}]", count));
    }

    private String generateInsertQuery(TableMetadata tableMetadata) {
        var baseQuery = format(
                "INSERT INTO %s.%s(%s) VALUES ",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                String.join(", ", tableMetadata.getColumnNames())
        );

        String valuePlaceholders = "(" +
                IntStream.rangeClosed(1, tableMetadata.getNumberOfColumns())
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
                    var dbType = column.getDbType();

                    if (PgType.TIMESTAMP_WITHOUT_TIME_ZONE.getDbType().equals(dbType)) {
                        var rawTimestamp = body.getString(columnName);
                        return PgType.TIMESTAMP_WITHOUT_TIME_ZONE.parse(rawTimestamp);
                    } else if (PgType.TIMESTAMP_WITH_TIME_ZONE.getDbType().equals(dbType)) {
                        var rawTimestamp = body.getString(columnName);
                        return PgType.TIMESTAMP_WITH_TIME_ZONE.parse(rawTimestamp);
                    }

                    return body.getValue(columnName);
                })
                .collect(Collectors.toList()));
    }

    private Future<Void> validaUpdateRequest(TableMetadata tableMetadata, JsonObject data, Map<String, String> pathParams) {

        if (tableMetadata.getNumberOfColumns() == 1) {
            return Future.failedFuture(new IllegalStateException("Unable to update table with only one column"));
        }

        var resourceIdInPath = pathParams.get(tableMetadata.getPkColumnName());
        var resourceIdInBody = data.getString(tableMetadata.getPkColumnName());

        if (resourceIdInBody.equals(resourceIdInPath)) {
            return Future.succeededFuture();
        }

        return Future.failedFuture(new InconsistentStateException());
    }

    private String generateColumnsToUpdate(List<ColumnMetadata> columns) {
        return IntStream.range(0, columns.size())
                .mapToObj(i -> format("%s = $%d", columns.get(i).getColumnName(), i + 2))
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
}
