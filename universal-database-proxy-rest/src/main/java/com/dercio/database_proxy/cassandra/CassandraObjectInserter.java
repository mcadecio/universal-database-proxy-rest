package com.dercio.database_proxy.cassandra;

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
import java.util.stream.IntStream;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CassandraObjectInserter {

    private final CassandraClient cassandraClient;
    private final CassandraObjectFinder finder;

    /**
     * CQL has no RETURNING, so the Location id is rebuilt from the body. Cassandra never generates keys.
     */
    public Future<Object> create(CassandraTableMetadata tableMetadata, JsonObject data) {
        return CassandraStatements.execute(
                        cassandraClient,
                        generateInsertQuery(tableMetadata),
                        tableMetadata.generateValuesForInsert(data))
                .map(generateResourceId(tableMetadata, data));
    }

    /**
     * No affected-row count exists and INSERT is an upsert, so a missing row must be detected by reading.
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

                    // Nothing to SET, and CQL cannot assign a key column to itself as Postgres does.
                    if (tableMetadata.getNonPrimaryKeyColumns().isEmpty()) {
                        return Future.succeededFuture(1);
                    }

                    return CassandraStatements.execute(
                                    cassandraClient,
                                    generateUpdateQuery(tableMetadata),
                                    tableMetadata.generateValuesForUpdate(data, pathParams))
                            .map(1);
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

        var placeholders = IntStream.range(0, columnNames.size())
                .mapToObj(i -> "?")
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
