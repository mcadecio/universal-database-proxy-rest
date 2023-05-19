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
    private boolean isSslEnabled;
    private String sslCertPath;
}
