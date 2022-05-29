package com.dercio.database_proxy.proxy;

import com.dercio.database_proxy.common.verticle.Verticle;
import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.net.NetServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Verticle
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProxyVerticle extends AbstractVerticle {

    private final ProxyHandler proxyHandler;
    private final ProxyConfig proxyConfig;
    private NetServer netServer;

    @Override
    public void start(Promise<Void> startPromise) {
        this.netServer = vertx.createNetServer();

        netServer.connectHandler(proxyHandler);

        var sourceConfig = proxyConfig.getSource();
        netServer
                .listen(sourceConfig.getPort(), sourceConfig.getHost())
                .onSuccess(server -> log.info("TCP Server Started ... {}", server.actualPort()))
                .onSuccess(server -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        netServer.close(result -> stopPromise.complete());
    }
}
