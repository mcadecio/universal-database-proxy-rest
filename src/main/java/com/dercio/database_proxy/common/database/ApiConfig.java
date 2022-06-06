package com.dercio.database_proxy.common.database;

public interface ApiConfig {
    boolean isEnabled();

    String getHost();

    Integer getPort();

    String getOpenApiFilePath();

    DatabaseConfig getDatabase();
}
