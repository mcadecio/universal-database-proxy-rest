package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ColumnMetadata;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.postgres.type.PgType;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PgObjectDeleter {

    private final SqlClient sqlClient;

    public Future<Integer> deleteData(TableMetadata tableMetadata, Map<String, String> pathParams) {

        return sqlClient.preparedQuery(generateDeleteQuery(tableMetadata))
                .execute(findPkValues(tableMetadata, pathParams))
                .map(SqlResult::rowCount)
                .onSuccess(count -> log.info("Rows deleted [{}]", count));
    }

    private String generateDeleteQuery(TableMetadata tableMetadata) {
        var query = format(
                "DELETE FROM %s.%s WHERE %s RETURNING %s",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                generateColumnsToDeleteBy(tableMetadata.getPrimaryKeyColumns()),
                tableMetadata.getPkColumnName()
        );

        log.info("Generated delete query [{}]", query);

        return query;
    }

    private Tuple findPkValues(TableMetadata tableMetadata, Map<String, String> pathParams) {
        Tuple tuples = Tuple.tuple();
        tableMetadata.getPrimaryKeyColumns()
                .stream()
                .filter(column -> pathParams.containsKey(column.getColumnName()))
                .map(column -> PgType.parse(column.getDbType(), pathParams.get(column.getColumnName())))
                .forEach(tuples::addValue);
        return tuples;
    }

    private String generateColumnsToDeleteBy(List<ColumnMetadata> columns) {
        return IntStream.range(0, columns.size())
                .mapToObj(i -> format("%s = $%d", columns.get(i).getColumnName(), i + 1))
                .collect(Collectors.joining(" AND "));
    }
}
