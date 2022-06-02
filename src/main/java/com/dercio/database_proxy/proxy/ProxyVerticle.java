package com.dercio.database_proxy.proxy;

import com.dercio.database_proxy.common.verticle.Verticle;
import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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

        vertx.eventBus()
                .<ProxyRequest>consumer("proxy.server")
                .handler(message -> message.body()
                        .getAction()
                        .on(ProxyAction.OPEN, this::openServer)
                        .on(ProxyAction.CLOSE, this::closeServer)
                );

        netServer = vertx.createNetServer().connectHandler(proxyHandler);

        openServer()
                .onSuccess(startPromise::complete)
                .onFailure(startPromise::fail);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        closeServer()
                .onSuccess(stopPromise::complete)
                .onFailure(stopPromise::fail);
    }

    private Future<Void> openServer() {
        var sourceConfig = proxyConfig.getSource();
        return netServer
                .close()
                .compose(unused -> netServer.listen(sourceConfig.getPort(), sourceConfig.getHost()))
                .onSuccess(server -> log.info("TCP Server Started ... {}", server.actualPort()))
                .mapEmpty();
    }

    private Future<Void> closeServer() {
        log.info("TCP Server Stopped");
        return netServer.close();
    }
}
