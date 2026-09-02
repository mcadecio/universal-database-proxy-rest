package com.dercio.database_proxy.cassandra;

import com.dercio.database_proxy.common.database.ConfigValues;
import com.dercio.database_proxy.common.database.DatabaseConfig;
import io.vertx.cassandra.CassandraClientOptions;

import java.util.Map;

public class CassandraClientOptionsFactory {

    public CassandraClientOptions create(DatabaseConfig databaseConfig, Map<String, String> envVariables) {
        var username = ConfigValues.resolve(databaseConfig.getUsername(), envVariables);
        var password = ConfigValues.resolve(databaseConfig.getPassword(), envVariables);
        var keyspace = ConfigValues.resolve(databaseConfig.getDatabaseName(), envVariables);
        var localDatacenter = resolveLocalDatacenter(databaseConfig, envVariables);

        var options = new CassandraClientOptions().setKeyspace(keyspace);

        ConfigValues.resolveHosts(databaseConfig, envVariables)
                .forEach(contactPoint -> addContactPoint(options, contactPoint));

        if (isNotBlank(username)) {
            options.setUsername(username).setPassword(password);
        }

        options.dataStaxClusterBuilder().withLocalDatacenter(localDatacenter);

        return options;
    }

    private void addContactPoint(CassandraClientOptions options, String contactPoint) {
        var separator = contactPoint.lastIndexOf(':');

        if (separator < 1 || separator == contactPoint.length() - 1) {
            throw new IllegalStateException(
                    "Cassandra contact point [" + contactPoint + "] must be in host:port form");
        }

        options.addContactPoint(
                contactPoint.substring(0, separator),
                Integer.parseInt(contactPoint.substring(separator + 1))
        );
    }

    private String resolveLocalDatacenter(DatabaseConfig databaseConfig, Map<String, String> envVariables) {
        var configured = ConfigValues.resolve(databaseConfig.getLocalDatacenter(), envVariables);

        if (isNotBlank(configured)) {
            return configured;
        }

        throw new IllegalStateException(
                "cassandraApi.database.localDatacenter is required: the DataStax driver cannot route "
                        + "without it.");
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
