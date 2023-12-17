package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.postgres.type.PgType;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
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
                .execute(generateTupleForInsert(tableMetadata, data, Map.of()))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .map(json-> json.getMap().values())
                        .flatMap(Collection::stream)
                        .map(Object::toString)
                        .collect(Collectors.joining(":")));
    }

    public Future<Integer> update(TableMetadata tableMetadata, JsonObject data, Map<String, String> pathParams) {

        return sqlClient.preparedQuery(generateUpdateQuery(tableMetadata))
                        .execute(generateTupleForInsert(tableMetadata, data, pathParams))
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
                ") RETURNING " + String.join(",", tableMetadata.getPrimaryKeyColumnNames());

        var finalQuery = baseQuery + valuePlaceholders;

        log.info("Generated insert query {}", finalQuery);

        return finalQuery;
    }

    private Tuple generateTupleForInsert(TableMetadata tableMetadata, JsonObject body, Map<String, String> pathParams) {

        var tuples = Tuple.tuple();


        tableMetadata.getColumns()
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
                .forEach(tuples::addValue);

        tableMetadata.getPrimaryKeyColumns()
                .stream()
                .filter(column -> pathParams.containsKey(column.getColumnName()))
                .map(column -> PgType.parse(column.getDbType(), pathParams.get(column.getColumnName())))
                .forEach(tuples::addValue);

        return tuples;
    }

    private String generateColumnsToUpdate(List<ColumnMetadata> columns) {
        return IntStream.range(0, columns.size())
                .mapToObj(i -> format("%s = $%d", columns.get(i).getColumnName(), i + 1))
                .collect(Collectors.joining(", "));
    }

    private String generateColumnsToUpdate(int startingIndex, List<ColumnMetadata> columns) {
        return IntStream.range(startingIndex, startingIndex + columns.size())
                .mapToObj(i -> format("%s = $%d", columns.get(i - startingIndex).getColumnName(), i + 1))
                .collect(Collectors.joining(" AND "));
    }

    private String generateUpdateQuery(TableMetadata tableMetadata) {
        var allColumns = tableMetadata.getColumns();
        var valuesPlaceholders = generateColumnsToUpdate(allColumns);
        var wherePredicates = generateColumnsToUpdate(allColumns.size(), tableMetadata.getPrimaryKeyColumns());
        var query = format("UPDATE %s.%s SET %s WHERE %s RETURNING %s",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                valuesPlaceholders,
                wherePredicates,
                tableMetadata.getPkColumnName()
        );

        log.info("Generated update query {}", query);

        return query;
    }
}
