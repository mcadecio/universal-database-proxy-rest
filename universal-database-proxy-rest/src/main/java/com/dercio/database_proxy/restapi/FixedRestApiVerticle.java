package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.router.RouterFactory;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FixedRestApiVerticle extends RestApiVerticle {

    public FixedRestApiVerticle(
            RouterFactory routerFactory,
            RestApiHandler restApiHandler,
            Repository pgRepository,
            ApiConfig apiConfig
    ) {
        super(routerFactory, restApiHandler, pgRepository, apiConfig);
    }

    @Override
    public Future<Handler<HttpServerRequest>> getHttpRequestHandler() {
        Promise<Handler<HttpServerRequest>> promise = Promise.promise();
        createRouter()
                .onSuccess(promise::complete)
                .onFailure(promise::fail);

        return promise.future()
                .onFailure(event -> log.error(event.getMessage()));
    }
}