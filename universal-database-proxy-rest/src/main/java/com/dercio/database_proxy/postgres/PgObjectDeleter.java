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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PgObjectDeleter {

    private final SqlClient sqlClient;

    public Future<Integer> deleteData(PgTableMetadata tableMetadata, Map<String, String> queryParams) {

        String deleteQuery = generateDeleteQuery(tableMetadata, queryParams.keySet());

        return sqlClient.preparedQuery(deleteQuery)
                .execute(tableMetadata.parseRawValues(queryParams))
                .map(SqlResult::rowCount)
                .onSuccess(count -> log.info("Rows deleted [{}]", count));
    }

    private String generateDeleteQuery(PgTableMetadata tableMetadata, Set<String> filters) {
        String baseQuery = "DELETE FROM " + tableMetadata.getQualifiedTableName();

        if (filters.isEmpty()) {
            log.info("Generated delete query [{}]", baseQuery);
            return baseQuery;
        }

        List<String> columnsToDeleteBy = tableMetadata.getColumns()
                .stream()
                .map(ColumnMetadata::getColumnName)
                .filter(filters::contains)
                .toList();

        String wherePredicates = generateColumnsToDeleteBy(columnsToDeleteBy);

        String finalQuery = baseQuery + " WHERE " + wherePredicates;

        log.info("Generated delete query [{}]", finalQuery);

        return finalQuery;
    }

    private String generateColumnsToDeleteBy(List<String> columnNames) {
        return IntStream.range(0, columnNames.size())
                .mapToObj(i -> format("%s = $%d", columnNames.get(i), i + 1))
                .collect(Collectors.joining(" AND "));
    }
}
