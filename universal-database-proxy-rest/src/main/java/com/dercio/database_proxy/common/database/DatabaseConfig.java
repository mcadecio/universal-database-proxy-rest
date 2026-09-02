package com.dercio.database_proxy.common.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfig {
    private List<String> hosts;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String databaseName;
    private String jdbcUrl;
    private String targetServerType;
    private boolean sslEnabled;
    private String sslCertPath;
    private String localDatacenter;
}
