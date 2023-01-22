package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.router.RouterFactory;

public class RestApiVerticleSelector {

    public RestApiVerticle select(
            RouterFactory routerFactory,
            RestApiHandler restApiHandler,
            Repository pgRepository,
            ApiConfig apiConfig
    ) {
        if (apiConfig.getStartupDelay() > 0) {
            return new DelayedStartupRestApiVerticle(routerFactory, restApiHandler, pgRepository, apiConfig);
        } else if (apiConfig.getReloadFrequency() > 0) {
            return new DynamicRestApiVerticle(routerFactory, restApiHandler, pgRepository, apiConfig);
        } else {
            return new FixedRestApiVerticle(routerFactory, restApiHandler, pgRepository, apiConfig);
        }
    }
}
