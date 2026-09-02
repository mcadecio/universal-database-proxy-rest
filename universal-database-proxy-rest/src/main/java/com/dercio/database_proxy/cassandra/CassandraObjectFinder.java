package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.dercio.database_proxy.postgres.InconsistentStateException;
import com.google.inject.Inject;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CassandraObjectFinder {

    private final CassandraClient cassandraClient;
    private final boolean allowFiltering;

    public Future<List<JsonObject>> find(CassandraTableMetadata tableMetadata, Map<String, String> queryFilters) {
        return findRows(tableMetadata, queryFilters)
                .map(tableMetadata::normalizeRows)
                .onSuccess(items -> log.info("Retrieved [{}] rows", items.size()));
    }

    public Future<List<Row>> findRows(CassandraTableMetadata tableMetadata, Map<String, String> queryFilters) {
        String selectQuery;
        try {
            selectQuery = generateSelectQuery(tableMetadata, queryFilters);
        } catch (InconsistentStateException e) {
            return Future.failedFuture(e);
        }

        return CassandraStatements.execute(cassandraClient, selectQuery, tableMetadata.parseRawValues(queryFilters));
    }

    public Future<Boolean> existsByPrimaryKey(CassandraTableMetadata tableMetadata, Map<String, String> pathParams) {
        var query = format("SELECT %s FROM %s WHERE %s LIMIT 1",
                String.join(", ", tableMetadata.getTableMetadata().getPrimaryKeyColumnNames()),
                tableMetadata.getQualifiedTableName(),
                tableMetadata.getPrimaryKeyColumns()
                        .stream()
                        .map(column -> format("%s = ?", column.getColumnName()))
                        .collect(Collectors.joining(" AND "))
        );

        return CassandraStatements.execute(cassandraClient, query, tableMetadata.primaryKeyValues(pathParams))
                .map(rows -> !rows.isEmpty());
    }

    private String generateSelectQuery(CassandraTableMetadata tableMetadata, Map<String, String> queryFilters) {
        var baseQuery = format("SELECT * FROM %s", tableMetadata.getQualifiedTableName());

        var columnsToFilterBy = tableMetadata.filterableColumnNames(queryFilters.keySet());

        if (columnsToFilterBy.isEmpty()) {
            return baseQuery;
        }

        var wherePredicates = columnsToFilterBy.stream()
                .map(column -> filterPredicate(tableMetadata, column))
                .collect(Collectors.joining(" AND "));

        var query = baseQuery + " WHERE " + wherePredicates;

        if (tableMetadata.requiresAllowFiltering(columnsToFilterBy)) {
            if (!allowFiltering) {
                throw new InconsistentStateException(format(
                        "Filtering by %s requires a full scan of table [%s], which is disabled. "
                                + "Restrict the query by the primary key instead.",
                        columnsToFilterBy, tableMetadata.getQualifiedTableName()));
            }
            query = query + " ALLOW FILTERING";
        }

        log.info("Generated select query [{}]", query);

        return query;
    }

    private String filterPredicate(CassandraTableMetadata tableMetadata, String columnName) {
        return tableMetadata.isSetColumn(columnName)
                ? format("%s CONTAINS ?", columnName)
                : format("%s = ?", columnName);
    }
}
