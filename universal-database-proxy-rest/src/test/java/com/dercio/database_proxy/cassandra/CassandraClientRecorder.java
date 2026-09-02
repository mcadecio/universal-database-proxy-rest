package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Records the CQL and bound values passed through a mocked {@link CassandraClient}. Every statement
 * is prepared, so what a test needs to assert on is the query string handed to {@code prepare} and
 * the values handed to {@code bind}, in invocation order.
 */
final class CassandraClientRecorder {

    private final List<String> queries = new ArrayList<>();
    private final List<List<Object>> boundValues = new ArrayList<>();

    static CassandraClientRecorder record(CassandraClient client, List<Row> rows) {
        var recorder = recordPrepares(client);

        when(client.executeWithFullFetch(any(Statement.class))).thenReturn(Future.succeededFuture(rows));

        return recorder;
    }

    static CassandraClientRecorder record(CassandraClient client) {
        return record(client, List.of());
    }

    /** Answers consecutive executions with successive result sets. */
    @SafeVarargs
    static CassandraClientRecorder recordSequence(CassandraClient client, List<Row>... results) {
        var recorder = recordPrepares(client);

        var stubbing = when(client.executeWithFullFetch(any(Statement.class)));
        for (List<Row> result : results) {
            stubbing = stubbing.thenReturn(Future.succeededFuture(result));
        }

        return recorder;
    }

    private static CassandraClientRecorder recordPrepares(CassandraClient client) {
        var recorder = new CassandraClientRecorder();

        when(client.prepare(anyString())).thenAnswer(invocation -> {
            recorder.queries.add(invocation.getArgument(0));

            var prepared = mock(PreparedStatement.class);
            when(prepared.bind(any(Object[].class))).thenAnswer(bind -> {
                recorder.boundValues.add(flatten(bind.getArguments()));
                return mock(BoundStatement.class);
            });

            return Future.succeededFuture(prepared);
        });

        return recorder;
    }

    private static List<Object> flatten(Object[] arguments) {
        if (arguments.length == 1 && arguments[0] instanceof Object[] varargs) {
            return Arrays.asList(varargs);
        }

        return Arrays.asList(arguments);
    }

    String query() {
        return queries.getFirst();
    }

    List<String> queries() {
        return queries;
    }

    List<Object> values() {
        return boundValues.isEmpty() ? List.of() : boundValues.getFirst();
    }

    List<List<Object>> allValues() {
        return boundValues;
    }
}
