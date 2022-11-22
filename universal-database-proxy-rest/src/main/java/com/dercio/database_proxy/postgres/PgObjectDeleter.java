package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

import static com.dercio.database_proxy.postgres.PgTypeMapper.INTEGER;
import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PgObjectDeleter {

    private final SqlClient sqlClient;

    public Future<Integer> deleteData(TableMetadata tableMetadata, Map<String, String> pathParams) {

        return sqlClient.preparedQuery(generateDeleteQuery(tableMetadata))
                .execute(Tuple.of(findPkValue(tableMetadata, pathParams)))
                .map(SqlResult::rowCount)
                .onSuccess(count -> log.info("Rows deleted [{}]", count));
    }

    private String generateDeleteQuery(TableMetadata tableMetadata) {

        var query = format(
                "DELETE FROM %s.%s WHERE %s = $1 RETURNING %s",
                tableMetadata.getSchemaName(),
                tableMetadata.getTableName(),
                tableMetadata.getPkColumnName(),
                tableMetadata.getPkColumnName()
        );

        log.info("Generated delete query [{}]", query);

        return query;
    }

    private Object findPkValue(TableMetadata tableMetadata, Map<String, String> pathParams) {
        var primaryKeyColumn = tableMetadata.getPrimaryKeyColumn();
        var value = pathParams.get(primaryKeyColumn.getColumnName());
        return primaryKeyColumn.getOpenApiType().equals(INTEGER) ? Long.parseLong(value) : value;
    }
}
