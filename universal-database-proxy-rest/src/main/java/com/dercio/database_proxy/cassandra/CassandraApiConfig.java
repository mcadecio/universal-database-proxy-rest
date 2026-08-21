package com.dercio.database_proxy.cassandra;

import com.dercio.database_proxy.common.configuration.Configuration;
import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.DatabaseConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration(name = "cassandraApi")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CassandraApiConfig implements ApiConfig {
    private boolean enabled;
    private String host;
    private Integer port;
    private String openApiFilePath;
    private DatabaseConfig database;
    private long startupDelay;
    private long reloadFrequency;

    /**
     * CQL rejects a {@code WHERE} on anything the primary key cannot satisfy unless
     * {@code ALLOW FILTERING} is appended, which triggers a cluster-wide scan. Left on by default so
     * filtering behaves like the Postgres API; set to {@code false} on a production cluster to get a
     * 400 instead of a scan.
     */
    private boolean allowFiltering = true;
}
