package com.dercio.database_proxy.cockroach;

import com.dercio.database_proxy.common.handlers.FailureHandler;
import com.dercio.database_proxy.common.handlers.NotFoundHandler;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.common.module.Module;
import com.dercio.database_proxy.postgres.PgRepository;
import com.dercio.database_proxy.restapi.RestApiHandler;
import com.dercio.database_proxy.restapi.RestApiVerticle;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;

@Module
public class CockroachModule extends AbstractModule {

    @ProvidesIntoSet
    AbstractVerticle crbApiVerticle(
            FailureHandler failureHandler,
            NotFoundHandler notFoundHandler,
            Vertx vertx,
            CrbApiConfig apiConfig,
            Mapper mapper
    ) {

        var pgRepository = new PgRepository(createCrbClients(vertx, apiConfig));

        return new RestApiVerticle(
                failureHandler,
                notFoundHandler,
                new RestApiHandler(mapper, pgRepository),
                pgRepository,
                apiConfig
        );
    }

    private SqlClient createCrbClients(Vertx vertx, CrbApiConfig apiConfig) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        var databaseConfig = apiConfig.getDatabase();
        var password = System.getenv()
                .getOrDefault(databaseConfig.getPassword(), databaseConfig.getPassword());

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(databaseConfig.getPort())
                .setHost(databaseConfig.getHost())
                .setDatabase(databaseConfig.getDatabaseName())
                .setUser(databaseConfig.getUsername())
                .setPassword(password);

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(5);

        return PgPool.pool(vertx, connectOptions, poolOptions);
    }
}
