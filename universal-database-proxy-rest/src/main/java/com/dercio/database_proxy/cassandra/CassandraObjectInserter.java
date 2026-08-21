package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.dercio.database_proxy.common.database.ColumnMetadata;
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
public class CassandraObjectInserter {

    private final CassandraClient cassandraClient;
    private final CassandraObjectFinder finder;

    /**
     * CQL has no {@code RETURNING}, so the id used for the {@code Location} header is rebuilt from the
     * primary key values in the request body. Cassandra never generates keys, so the client always
     * supplies them.
     */
    public Future<Object> create(CassandraTableMetadata tableMetadata, JsonObject data) {
        var insertQuery = generateInsertQuery(tableMetadata);
        var values = tableMetadata.generateValuesForInsert(data).toArray();

        return cassandraClient.executeWithFullFetch(SimpleStatement.newInstance(insertQuery, values))
                .map(rows -> generateResourceId(tableMetadata, data));
    }

    /**
     * Cassandra reports no affected-row count, so existence is established with a read before the
     * write. Without it a {@code PUT} on a missing row would upsert it and answer 204 where the
     * Postgres API answers 404.
     */
    public Future<Integer> update(
            CassandraTableMetadata tableMetadata,
            JsonObject data,
            Map<String, String> pathParams
    ) {
        return finder.existsByPrimaryKey(tableMetadata, pathParams)
                .compose(exists -> {
                    if (Boolean.FALSE.equals(exists)) {
                        return Future.succeededFuture(0);
                    }

                    // A table whose columns are all part of the primary key has nothing to SET, and
                    // CQL cannot assign a primary key column to itself the way the Postgres
                    // implementation does. The existence check alone answers the request.
                    if (tableMetadata.getNonPrimaryKeyColumns().isEmpty()) {
                        return Future.succeededFuture(1);
                    }

                    var updateQuery = generateUpdateQuery(tableMetadata);
                    var values = tableMetadata.generateValuesForUpdate(data, pathParams).toArray();

                    return cassandraClient
                            .executeWithFullFetch(SimpleStatement.newInstance(updateQuery, values))
                            .map(rows -> 1);
                })
                .onSuccess(count -> log.info("Rows updated [{}]", count));
    }

    private Object generateResourceId(CassandraTableMetadata tableMetadata, JsonObject data) {
        return tableMetadata.getTableMetadata().getPrimaryKeyColumnNames()
                .stream()
                .map(data::getValue)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
    }

    private String generateInsertQuery(CassandraTableMetadata tableMetadata) {
        var columnNames = tableMetadata.getTableMetadata().getColumnNames();

        var placeholders = columnNames.stream()
                .map(column -> "?")
                .collect(Collectors.joining(","));

        var query = format("INSERT INTO %s(%s) VALUES (%s)",
                tableMetadata.getQualifiedTableName(),
                String.join(", ", columnNames),
                placeholders
        );

        log.info("Generated insert query {}", query);

        return query;
    }

    private String generateUpdateQuery(CassandraTableMetadata tableMetadata) {
        var setClause = tableMetadata.getNonPrimaryKeyColumns()
                .stream()
                .map(column -> format("%s = ?", column.getColumnName()))
                .collect(Collectors.joining(", "));

        var query = format("UPDATE %s SET %s WHERE %s",
                tableMetadata.getQualifiedTableName(),
                setClause,
                generateWherePredicates(tableMetadata.getPrimaryKeyColumns())
        );

        log.info("Generated update query {}", query);

        return query;
    }

    private String generateWherePredicates(List<ColumnMetadata> columns) {
        return columns.stream()
                .map(column -> format("%s = ?", column.getColumnName()))
                .collect(Collectors.joining(" AND "));
    }
}
