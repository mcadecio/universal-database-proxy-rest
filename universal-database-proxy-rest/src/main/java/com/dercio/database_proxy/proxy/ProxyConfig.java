package com.dercio.database_proxy.proxy;

import com.dercio.database_proxy.common.configuration.Configuration;
import io.vertx.core.net.ProxyOptions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Configuration(name = "proxy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProxyConfig {
    private boolean enabled;
    private ProxyOptions httpServer;
    private ProxyOptions source;
    private ProxyOptions destination;
}
