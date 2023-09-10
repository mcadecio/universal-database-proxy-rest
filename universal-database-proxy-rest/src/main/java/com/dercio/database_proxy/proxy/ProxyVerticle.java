package com.dercio.database_proxy.proxy;

import com.dercio.database_proxy.common.exceptions.VerticleDisabledException;
import com.dercio.database_proxy.common.handlers.FailureHandler;
import com.dercio.database_proxy.common.handlers.NotFoundHandler;
import com.dercio.database_proxy.common.verticle.Verticle;
import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.NetServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static com.simplaex.http.StatusCode._200;

@Log4j2
@Verticle
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProxyVerticle extends AbstractVerticle {

    private final ProxyHandler proxyHandler;
    private final ProxyConfig proxyConfig;
    private final FailureHandler failureHandler;
    private final NotFoundHandler notFoundHandler;
    private NetServer netServer;
    private HttpServer httpServer;

    @Override
    public void start(Promise<Void> startPromise) {
        if (!proxyConfig.isEnabled()) {
            startPromise.fail(new VerticleDisabledException(getClass().getSimpleName()));
            return;
        }

        httpServer = vertx.createHttpServer();
        netServer = vertx.createNetServer().connectHandler(proxyHandler);

        Future.all(startHttpServer(), openNetServer())
                .onSuccess(event -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        Future.all(stopHttpServer(), closeNetServer())
                .onSuccess(event -> stopPromise.complete())
                .onFailure(stopPromise::fail);
    }

    private Future<Void> startHttpServer() {
        final var router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/proxy").handler(event -> {
            event.body()
                    .asJsonObject()
                    .mapTo(ProxyRequest.class)
                    .getAction()
                    .on(ProxyAction.OPEN, this::openNetServer)
                    .on(ProxyAction.CLOSE, this::closeNetServer);

            event.response().setStatusCode(_200.getCode()).end();
        });

        router.route().failureHandler(failureHandler);
        router.route().last().handler(notFoundHandler);

        return httpServer
                .requestHandler(router)
                .listen(proxyConfig.getHttpServer().getPort(), proxyConfig.getHttpServer().getHost())
                .onSuccess(event -> log.info("HTTP Server Started ... {}", event.actualPort()))
                .mapEmpty();
    }

    private Future<Void> stopHttpServer() {
        return httpServer.close()
                .onSuccess(event -> log.info("Goodbye HTTP server ..."))
                .onFailure(error -> log.error(error.getMessage()));
    }

    private Future<Void> openNetServer() {
        var sourceConfig = proxyConfig.getSource();
        return netServer
                .close()
                .compose(unused -> netServer.listen(sourceConfig.getPort(), sourceConfig.getHost()))
                .onSuccess(server -> log.info("TCP Server Started ... {}", server.actualPort()))
                .mapEmpty();
    }

    private Future<Void> closeNetServer() {
        log.info("TCP Server Stopped");
        return netServer.close();
    }
}
