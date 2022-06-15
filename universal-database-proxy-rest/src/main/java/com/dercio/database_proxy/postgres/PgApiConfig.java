package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.configuration.Configuration;
import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.DatabaseConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Configuration(name = "postgresApi")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PgApiConfig implements ApiConfig {
    private boolean enabled;
    private String host;
    private Integer port;
    private String openApiFilePath;
    private DatabaseConfig database;
}
