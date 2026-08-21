package com.dercio.database_proxy.common.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfig {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String databaseName;
    private String jdbcUrl;
    private String targetServerType;
    private boolean sslEnabled;
    private String sslCertPath;
    // Cassandra only: the DataStax driver requires a local datacenter whenever contact points are given.
    private String localDatacenter;
}
