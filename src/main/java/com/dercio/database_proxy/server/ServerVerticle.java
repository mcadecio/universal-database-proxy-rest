package com.dercio.database_proxy.server;

import com.dercio.database_proxy.common.verticle.Verticle;
import com.dercio.database_proxy.server.handlers.*;
import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Verticle
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ServerVerticle extends AbstractVerticle {

    private final RequestLoggingHandler requestLoggingHandler;
    private final HealthHandler healthHandler;
    private final FailureHandler failureHandler;
    private final NotFoundHandler notFoundHandler;
    private HttpServer httpServer;

    @Override
    public void start(Promise<Void> startPromise) {
        var httpConfig = new HttpConfig();
        final var router = Router.router(vertx);
        httpConfig.configureRouter(router);

        router.get("/health")
                .handler(requestLoggingHandler::logRequestReceipt)
                .handler(healthHandler)
                .handler(requestLoggingHandler::logResponseDispatch);

        router.route().failureHandler(failureHandler);
        router.route().last().handler(notFoundHandler);

        httpServer = vertx.createHttpServer();
        httpServer
                .requestHandler(router)
                .listen(httpConfig.getPort())
                .onSuccess(event -> log.info("HTTP Server Started ... {}", event.actualPort()))
                .onFailure(error -> log.error("Error starting up", error))
                .onComplete(event -> startPromise.complete());
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        httpServer.close()
                .onSuccess(event -> log.info("Goodbye HTTP server ..."))
                .onFailure(error -> log.error(error.getMessage()))
                .onComplete(event -> stopPromise.complete());
    }

}
