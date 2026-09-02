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

    /** Permits filters the primary key cannot satisfy, which CQL only serves with a cluster-wide scan. */
    private boolean allowFiltering;

    private String localDatacenter;
}
