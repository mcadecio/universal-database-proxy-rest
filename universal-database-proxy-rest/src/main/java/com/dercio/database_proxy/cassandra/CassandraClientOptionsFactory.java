package com.dercio.database_proxy.cassandra;

import com.dercio.database_proxy.common.database.ConfigValues;
import com.dercio.database_proxy.common.database.DatabaseConfig;
import io.vertx.cassandra.CassandraClientOptions;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Log4j2
public class CassandraClientOptionsFactory {

    /** The datacenter name a single-node Cassandra reports out of the box. */
    static final String DEFAULT_LOCAL_DATACENTER = "datacenter1";

    public CassandraClientOptions create(DatabaseConfig databaseConfig, Map<String, String> envVariables) {
        var host = ConfigValues.resolve(databaseConfig.getHost(), envVariables);
        var username = ConfigValues.resolve(databaseConfig.getUsername(), envVariables);
        var password = ConfigValues.resolve(databaseConfig.getPassword(), envVariables);
        var keyspace = ConfigValues.resolve(databaseConfig.getDatabaseName(), envVariables);
        var localDatacenter = resolveLocalDatacenter(databaseConfig, envVariables);

        var options = new CassandraClientOptions()
                .addContactPoint(host, databaseConfig.getPort())
                .setKeyspace(keyspace);

        // Cassandra runs without authentication by default; sending empty credentials fails the
        // handshake, so only configure them when a username is actually supplied.
        if (isNotBlank(username)) {
            options.setUsername(username).setPassword(password);
        }

        // The DataStax driver refuses to start with contact points but no local datacenter.
        options.dataStaxClusterBuilder().withLocalDatacenter(localDatacenter);

        return options;
    }

    private String resolveLocalDatacenter(DatabaseConfig databaseConfig, Map<String, String> envVariables) {
        var configured = ConfigValues.resolve(databaseConfig.getLocalDatacenter(), envVariables);

        if (isNotBlank(configured)) {
            return configured;
        }

        log.info("No localDatacenter configured, defaulting to [{}]", DEFAULT_LOCAL_DATACENTER);

        return DEFAULT_LOCAL_DATACENTER;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
