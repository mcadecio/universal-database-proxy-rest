package com.dercio.database_proxy.common.database;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Config string values are treated as environment variable names first, falling back to the literal
 * string — {@code "password": "POSTGRES_PASSWORD"} reads {@code $POSTGRES_PASSWORD}, while
 * {@code "password": "admin"} is used as-is when no such variable exists.
 */
public final class ConfigValues {

    private ConfigValues() {
    }

    public static String resolve(String value, Map<String, String> envVariables) {
        if (value == null) return null;

        return envVariables.getOrDefault(value, value);
    }

    /**
     * The ordered {@code host:port} endpoints to connect to. Prefers {@code hosts}, falling back to
     * the single {@code host} and {@code port}. A resolved entry may itself hold a comma separated
     * list, so one environment variable can carry every endpoint.
     */
    public static List<String> resolveHosts(DatabaseConfig databaseConfig, Map<String, String> envVariables) {
        var hosts = databaseConfig.getHosts();

        if (hosts == null || hosts.isEmpty()) {
            var host = resolve(databaseConfig.getHost(), envVariables);

            if (host == null || host.isBlank()) {
                throw new IllegalStateException("No database hosts configured: set either 'hosts' or 'host' and 'port'");
            }

            return List.of(host + ":" + databaseConfig.getPort());
        }

        return hosts.stream()
                .map(entry -> resolve(entry, envVariables))
                .filter(entry -> entry != null && !entry.isBlank())
                .map(entry -> entry.split(","))
                .flatMap(Arrays::stream)
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .toList();
    }
}
