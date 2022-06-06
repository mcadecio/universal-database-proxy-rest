package com.dercio.database_proxy.cockroach;

import com.dercio.database_proxy.common.configuration.Configuration;
import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.DatabaseConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Configuration(name = "cockroachApi")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CrbApiConfig implements ApiConfig {
    private boolean enabled;
    private String host;
    private Integer port;
    private String openApiFilePath;
    private DatabaseConfig database;
}
