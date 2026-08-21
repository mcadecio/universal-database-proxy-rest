package com.dercio.database_proxy.common.database;

import java.util.Map;

/**
 * Config string values for connection details are treated as <b>environment variable names first,
 * falling back to the literal string</b> — e.g. {@code "password": "POSTGRES_PASSWORD"} reads
 * {@code $POSTGRES_PASSWORD}, while {@code "password": "admin"} is used as-is when no such variable
 * exists. Every database's connection factory must resolve values through here so the behaviour
 * stays consistent across engines.
 */
public final class ConfigValues {

    private ConfigValues() {
    }

    public static String resolve(String value, Map<String, String> envVariables) {
        if (value == null) return null;

        return envVariables.getOrDefault(value, value);
    }
}
