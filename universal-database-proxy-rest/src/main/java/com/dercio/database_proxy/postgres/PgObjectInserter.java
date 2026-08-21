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
        PgTableMetadata pgTableMetadata = new PgTableMetadata(tableMetadata);
        return sqlClient.preparedQuery(generateInsertQuery(tableMetadata))
                .execute(pgTableMetadata.generateTupleForInsert(data))
                .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                        .map(Row::toJson)
                        .map(json-> json.getMap().values())
                        .flatMap(Collection::stream)
                        .map(Object::toString)
                        .collect(Collectors.joining(":")));
    }

    public Future<Integer> update(TableMetadata tableMetadata, JsonObject data, Map<String, String> pathParams) {
        PgTableMetadata pgTableMetadata = new PgTableMetadata(tableMetadata);
        return sqlClient.preparedQuery(generateUpdateQuery(tableMetadata))
                        .execute(pgTableMetadata.generateTupleForUpdate(data, pathParams))
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
                IntStream.range(0, tableMetadata.getNumberOfColumns())
                        .mapToObj(i -> tableMetadata.getColumns().get(i).getDbType())
                        .map(PgType::placeholder)
                        .collect(Collectors.joining(",")) +
                ") RETURNING " + String.join(",", tableMetadata.getPrimaryKeyColumnNames());

        var finalQuery = baseQuery + valuePlaceholders;

        log.info("Generated insert query {}", finalQuery);

        return finalQuery;
    }

    private String generateColumnsToUpdate(List<ColumnMetadata> columns) {
        return columns.stream()
                .map(column -> {
                    String columnName = column.getColumnName();
                    String placeholder = PgType.placeholder(column.getDbType());
                    return "%s = %s".formatted(columnName, placeholder);
                })
                .collect(Collectors.joining(", "));
    }

    private String generateSetClause(TableMetadata tableMetadata) {
        var nonPrimaryKeyColumns = tableMetadata.getNonPrimaryKeyColumns();

        if (!nonPrimaryKeyColumns.isEmpty()) {
            return generateColumnsToUpdate(nonPrimaryKeyColumns);
        }

        // Tables whose columns are all part of the primary key have nothing to update. Assign the
        // primary key to itself so the statement stays valid and still reports row existence via
        // the row count (a no-op update), without ever changing the primary key value.
        return tableMetadata.getPrimaryKeyColumns().stream()
                .map(column -> format("%s = %s", column.getColumnName(), column.getColumnName()))
                .collect(Collectors.joining(", "));
    }

    private String generateWherePredicates(List<ColumnMetadata> columns) {
        return columns.stream()
                .map(column -> format("%s = ?", column.getColumnName()))
                .collect(Collectors.joining(" AND "));
    }

    private String generateUpdateQuery(TableMetadata tableMetadata) {
        var valuesPlaceholders = generateSetClause(tableMetadata);
        var wherePredicates = generateWherePredicates(tableMetadata.getPrimaryKeyColumns());
        var query = format("UPDATE %s.%s SET %s WHERE %s",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                valuesPlaceholders,
                wherePredicates
        );

        log.info("Generated update query {}", query);

        return query;
    }

}
