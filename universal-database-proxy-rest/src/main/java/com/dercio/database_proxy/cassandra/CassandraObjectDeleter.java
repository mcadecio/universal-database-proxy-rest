package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.google.inject.Inject;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CassandraObjectDeleter {

    private final CassandraClient cassandraClient;
    private final CassandraObjectFinder finder;

    /**
     * A DELETE by primary key always reports success in CQL, so the 404 has to come from a read.
     */
    public Future<Integer> deleteDataById(CassandraTableMetadata tableMetadata, Map<String, String> pathParams) {
        return finder.existsByPrimaryKey(tableMetadata, pathParams)
                .compose(exists -> {
                    if (Boolean.FALSE.equals(exists)) {
                        return Future.succeededFuture(0);
                    }

                    return CassandraStatements.execute(
                                    cassandraClient,
                                    deleteByPrimaryKeyQuery(tableMetadata),
                                    tableMetadata.primaryKeyValues(pathParams))
                            .map(1);
                })
                .onSuccess(count -> log.info("Rows deleted [{}]", count));
    }

    public Future<Integer> deleteData(CassandraTableMetadata tableMetadata, Map<String, String> queryParams) {
        if (tableMetadata.filterableColumnNames(queryParams.keySet()).isEmpty()) {
            return truncate(tableMetadata);
        }

        return deleteMatchingRows(tableMetadata, queryParams);
    }

    /**
     * CQL rejects a bare DELETE FROM, so this is a TRUNCATE, which reports no count and always answers 204.
     */
    private Future<Integer> truncate(CassandraTableMetadata tableMetadata) {
        var query = format("TRUNCATE %s", tableMetadata.getQualifiedTableName());

        log.info("Generated delete query [{}]", query);

        return CassandraStatements.execute(cassandraClient, query).map(1);
    }

    private Future<Integer> deleteMatchingRows(CassandraTableMetadata tableMetadata, Map<String, String> queryParams) {
        return finder.findRows(tableMetadata, queryParams)
                .compose(rows -> {
                    if (rows.isEmpty()) {
                        return Future.succeededFuture(0);
                    }

                    var deleteQuery = deleteByPrimaryKeyQuery(tableMetadata);

                    List<Future<?>> deletions = rows.stream()
                            .map(row -> deleteRow(tableMetadata, deleteQuery, row))
                            .collect(Collectors.toList());

                    return Future.all(deletions).map(ignored -> rows.size());
                })
                .onSuccess(count -> log.info("Rows deleted [{}]", count));
    }

    private Future<?> deleteRow(CassandraTableMetadata tableMetadata, String deleteQuery, Row row) {
        return CassandraStatements.execute(cassandraClient, deleteQuery, tableMetadata.primaryKeyValues(row));
    }

    private String deleteByPrimaryKeyQuery(CassandraTableMetadata tableMetadata) {
        var query = format("DELETE FROM %s WHERE %s",
                tableMetadata.getQualifiedTableName(),
                tableMetadata.getPrimaryKeyColumns()
                        .stream()
                        .map(ColumnMetadata::getColumnName)
                        .map(columnName -> format("%s = ?", columnName))
                        .collect(Collectors.joining(" AND "))
        );

        log.info("Generated delete query [{}]", query);

        return query;
    }
}
