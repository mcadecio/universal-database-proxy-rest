package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.router.RouterFactory;
import io.vertx.core.Promise;

public class DelayedStartupRestApiVerticle extends FixedRestApiVerticle {

    public DelayedStartupRestApiVerticle(
            RouterFactory routerFactory,
            RestApiHandler restApiHandler,
            Repository pgRepository,
            ApiConfig apiConfig
    ) {
        super(routerFactory, restApiHandler, pgRepository, apiConfig);
    }

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.setTimer(apiConfig.getStartupDelay(), timerId -> super.start(startPromise));
    }
}