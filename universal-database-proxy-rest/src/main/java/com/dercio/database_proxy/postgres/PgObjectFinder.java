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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PgObjectFinder {
    private final SqlClient sqlClient;

    public Future<List<JsonObject>> find(TableMetadata tableMetadata, Map<String, String> queryFilters) {
        return sqlClient.preparedQuery(generateSelectQuery(tableMetadata, queryFilters.keySet()))
                .execute(generateTupleForSelect(tableMetadata, queryFilters))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .toList())
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()));
    }

    public Future<Optional<JsonObject>> findById(TableMetadata tableMetadata, Map<String, String> pathParams) {
        Set<String> columsToFilterBy = tableMetadata.getPrimaryKeyColumns()
                .stream()
                .map(ColumnMetadata::getColumnName)
                .collect(Collectors.toSet());

        return sqlClient.preparedQuery(generateSelectQuery(tableMetadata, columsToFilterBy))
                .execute(generateTupleForSelect(tableMetadata, pathParams))
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .findFirst());
    }

    private String generateSelectQuery(TableMetadata tableMetadata, Set<String> filters) {

        String baseQuery = format("SELECT * FROM %s.%s", tableMetadata.getSchemaName(), tableMetadata.getTableName());

        if (filters.isEmpty()) {
            return baseQuery;
        }

        List<String> columnsToFilterBy = tableMetadata.getColumns()
                .stream()
                .map(ColumnMetadata::getColumnName)
                .filter(filters::contains)
                .toList();

        String wherePredicates = IntStream.range(0, columnsToFilterBy.size())
                .mapToObj(i -> format("%s = $%d", columnsToFilterBy.get(i), i + 1))
                .collect(Collectors.joining(" AND "));

        baseQuery = baseQuery + " WHERE " + wherePredicates;

        log.info("Generated select query [{}]", baseQuery);

        return baseQuery;
    }

    private Tuple generateTupleForSelect(TableMetadata tableMetadata, Map<String, String> queryFilters) {
        if (queryFilters.isEmpty()) {
            return Tuple.tuple();
        }

        List<Object> values = tableMetadata.getColumns()
                .stream()
                .filter(column -> queryFilters.containsKey(column.getColumnName()))
                .map(column -> {
                    var columnName = column.getColumnName();
                    var value = queryFilters.get(columnName);
                    return PgType.parse(column.getDbType(), value);
                })
                .toList();

        return Tuple.from(values);
    }

}
