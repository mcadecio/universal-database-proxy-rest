package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
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

/**
 * Builds every {@code SELECT} the Cassandra API issues, so the {@code ALLOW FILTERING} decision lives
 * in exactly one place.
 */
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

        var values = tableMetadata.parseRawValues(queryFilters).toArray();

        return cassandraClient.executeWithFullFetch(SimpleStatement.newInstance(selectQuery, values));
    }

    /**
     * A set column is filtered by membership — {@code tags CONTAINS ?} — because equality would
     * demand the caller spell out the whole set, which a query string cannot express well.
     */
    private String filterPredicate(CassandraTableMetadata tableMetadata, String columnName) {
        return tableMetadata.isSetColumn(columnName)
                ? format("%s CONTAINS ?", columnName)
                : format("%s = ?", columnName);
    }

    /**
     * Cassandra reports no affected-row count, so writes establish existence with a read first.
     * Restricting by the full primary key is always a single-partition read — never a scan.
     */
    public Future<Boolean> existsByPrimaryKey(CassandraTableMetadata tableMetadata, Map<String, String> pathParams) {
        var query = format("SELECT %s FROM %s WHERE %s LIMIT 1",
                String.join(", ", tableMetadata.getTableMetadata().getPrimaryKeyColumnNames()),
                tableMetadata.getQualifiedTableName(),
                tableMetadata.getPrimaryKeyColumns()
                        .stream()
                        .map(column -> format("%s = ?", column.getColumnName()))
                        .collect(Collectors.joining(" AND "))
        );

        var values = tableMetadata.primaryKeyValues(pathParams).toArray();

        return cassandraClient.executeWithFullFetch(SimpleStatement.newInstance(query, values))
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
}
