package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.cql.Row;
import io.vertx.cassandra.CassandraClient;
import io.vertx.core.Future;

import java.util.List;

/**
 * Preparing is what lets the driver see the partition key positions and route straight to a replica
 * rather than paying a coordinator hop. It caches by query string, so this costs one round trip per
 * distinct statement for the life of the session.
 */
final class CassandraStatements {

    private CassandraStatements() {
    }

    static Future<List<Row>> execute(CassandraClient client, String query, List<Object> values) {
        return client.prepare(query)
                .compose(prepared -> client.executeWithFullFetch(prepared.bind(values.toArray())));
    }

    static Future<List<Row>> execute(CassandraClient client, String query) {
        return execute(client, query, List.of());
    }
}
