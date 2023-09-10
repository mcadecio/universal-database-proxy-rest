package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.postgres.type.PgType;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
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
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PgObjectFinder {
    private final SqlClient sqlClient;

    public Future<List<JsonObject>> find(TableMetadata tableMetadata, MultiMap queryFilters) {

        return sqlClient.preparedQuery(generateSelectQuery(tableMetadata, queryFilters))
                .execute(generateTupleForSelect(tableMetadata, queryFilters))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .toList())
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()));
    }

    public Future<Optional<JsonObject>> findById(TableMetadata tableMetadata, Map<String, String> pathParams) {

        return sqlClient.preparedQuery(generateSelectByPkQuery(tableMetadata))
                .execute(Tuple.of(findPkValue(tableMetadata, pathParams)))
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .findFirst());
    }

    private Object findPkValue(TableMetadata tableMetadata, Map<String, String> pathParams) {
        var primaryKeyColumn = tableMetadata.getPrimaryKeyColumn();
        var value = pathParams.get(primaryKeyColumn.getColumnName());
        var dbType = primaryKeyColumn.getDbType();
        return PgType.parse(dbType, value);
    }

    private String generateSelectByPkQuery(TableMetadata tableMetadata) {

        var query = format(
                "SELECT * FROM %s.%s WHERE %s = $1",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                tableMetadata.getPkColumnName()
        );

        log.info("Generated select query [{}]", query);

        return query;
    }

    private String generateSelectQuery(TableMetadata tableMetadata, MultiMap queryFilters) {

        String baseQuery = format("SELECT * FROM %s.%s", tableMetadata.getSchemaName(), tableMetadata.getTableName());

        if (queryFilters.isEmpty()) {
            return baseQuery;
        }

        List<String> columnsToFilterBy = tableMetadata.getColumns()
                .stream()
                .map(ColumnMetadata::getColumnName)
                .filter(queryFilters::contains)
                .toList();

        String wherePredicates = IntStream.range(0, columnsToFilterBy.size())
                .mapToObj(i -> format("%s = $%d", columnsToFilterBy.get(i), i + 1))
                .collect(Collectors.joining(" AND "));

        baseQuery = baseQuery + " WHERE " + wherePredicates;

        log.info("Generated select query [{}]", baseQuery);

        return baseQuery;
    }

    private Tuple generateTupleForSelect(TableMetadata tableMetadata, MultiMap queryFilters) {
        if (queryFilters.isEmpty()) {
            return Tuple.tuple();
        }

        List<ColumnMetadata> columnsToFilterBy = tableMetadata.getColumns()
                .stream()
                .filter(column -> queryFilters.contains(column.getColumnName()))
                .toList();

        return Tuple.from(columnsToFilterBy
                .stream()
                .map(column -> {
                    var columnName = column.getColumnName();
                    var value = queryFilters.get(columnName);
                    return PgType.parse(column.getDbType(), value);
                })
                .toList());
    }

}
