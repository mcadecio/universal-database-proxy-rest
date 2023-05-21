package com.dercio.database_proxy.cockroach;

import com.dercio.database_proxy.common.configuration.Configuration;
import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.DatabaseConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration(name = "cockroachApi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrApiConfig implements ApiConfig {
    private boolean enabled;
    private String host;
    private Integer port;
    private String openApiFilePath;
    private DatabaseConfig database;
    private long startupDelay;
    private long reloadFrequency;
}
