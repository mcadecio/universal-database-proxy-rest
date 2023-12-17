package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PgObjectFinder {
    private final SqlClient sqlClient;

    public Future<List<JsonObject>> find(PgTableMetadata tableMetadata, Map<String, String> queryFilters) {
        return sqlClient.preparedQuery(generateSelectQuery(tableMetadata, queryFilters.keySet()))
                .execute(tableMetadata.parseRawValues(queryFilters))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .toList())
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()));
    }

    private String generateSelectQuery(PgTableMetadata tableMetadata, Set<String> filters) {

        String baseQuery = format("SELECT * FROM %s", tableMetadata.getQualifiedTableName());

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

}
