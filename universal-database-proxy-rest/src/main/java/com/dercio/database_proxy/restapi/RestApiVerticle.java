package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.exceptions.VerticleDisabledException;
import com.dercio.database_proxy.common.router.RouterFactory;
import com.dercio.database_proxy.common.router.RouterOptions;
import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public abstract class RestApiVerticle extends AbstractVerticle {

    private final RouterFactory routerFactory;
    protected final RestApiHandler restApiHandler;
    protected final Repository pgRepository;
    protected final ApiConfig apiConfig;
    private HttpServer httpServer;

    @Override
    public void start(Promise<Void> startPromise) {

        if (!apiConfig.isEnabled()) {
            startPromise.fail(new VerticleDisabledException(getClass().getSimpleName()));
            return;
        }

        var httpServerOptions = new HttpServerOptions().setUseAlpn(true).setSsl(false);

        getHttpRequestHandler().onSuccess(requestHandler -> {
            httpServer = vertx.createHttpServer(httpServerOptions);

            httpServer
                    .requestHandler(requestHandler)
                    .listen(apiConfig.getPort(), apiConfig.getHost())
                    .onSuccess(event -> log.info("Rest API Server Started ... {}", event.actualPort()))
                    .onSuccess(event -> startPromise.complete())
                    .onFailure(startPromise::fail);

        }).onFailure(startPromise::fail);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        httpServer.close()
                .onSuccess(event -> log.info("Goodbye Rest API server ..."))
                .onFailure(error -> log.error(error.getMessage()))
                .onComplete(event -> stopPromise.complete());
    }

    public abstract Future<Handler<HttpServerRequest>> getHttpRequestHandler();

    protected Future<Router> createRouter() {
        RouterOptions routerOptions = RouterOptions.builder(restApiHandler, apiConfig.getOpenApiFilePath())
                .build();

        return pgRepository.getTables(apiConfig.getDatabase().getDatabaseName())
                .compose(tables -> routerFactory.createRouter(tables, routerOptions))
                .onFailure(error -> log.error("Failed to create router: ", error));
    }

}
