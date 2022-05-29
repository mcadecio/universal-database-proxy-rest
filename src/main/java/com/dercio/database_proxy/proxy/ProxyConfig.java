package com.dercio.database_proxy.proxy;

import com.dercio.database_proxy.common.configuration.Configuration;
import io.vertx.core.net.ProxyOptions;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Configuration(name = "proxy")
@Getter
@NoArgsConstructor
public class ProxyConfig {
    private String name;
    private ProxyOptions source;
    private ProxyOptions destination;
}
