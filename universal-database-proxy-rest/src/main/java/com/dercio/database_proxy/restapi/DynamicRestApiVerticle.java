package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.router.RouterFactory;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class DynamicRestApiVerticle extends RestApiVerticle {

    private final AtomicReference<Handler<HttpServerRequest>> routerAtomicReference;

    public DynamicRestApiVerticle(
            RouterFactory routerFactory,
            RestApiHandler restApiHandler,
            Repository pgRepository,
            ApiConfig apiConfig
    ) {
        super(routerFactory, restApiHandler, pgRepository, apiConfig);
        routerAtomicReference = new AtomicReference<>();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        createRouter()
                .onSuccess(routerAtomicReference::set)
                .onSuccess(router -> super.start(startPromise))
                .onSuccess(router -> periodicallyReloadRouter())
                .onFailure(startPromise::fail);
    }

    @Override
    public Future<Handler<HttpServerRequest>> getHttpRequestHandler() {
        return Future.succeededFuture(httpServerRequest -> routerAtomicReference.get().handle(httpServerRequest));
    }

    private void periodicallyReloadRouter() {
        vertx.setPeriodic(apiConfig.getReloadFrequency(), t -> createRouter().onSuccess(routerAtomicReference::set));
    }
}
