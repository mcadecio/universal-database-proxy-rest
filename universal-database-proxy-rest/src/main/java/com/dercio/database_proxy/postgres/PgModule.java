package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.handlers.FailureHandler;
import com.dercio.database_proxy.common.handlers.NotFoundHandler;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.common.module.Module;
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
public class PgModule extends AbstractModule {

    @ProvidesIntoSet
    AbstractVerticle pgApiVerticle(
            FailureHandler failureHandler,
            NotFoundHandler notFoundHandler,
            Vertx vertx,
            PgApiConfig pgApiConfig,
            Mapper mapper
    ) {

        var pgRepository = new PgRepository(createPgClients(vertx, pgApiConfig));

        return new RestApiVerticle(
                failureHandler,
                notFoundHandler,
                new RestApiHandler(mapper, pgRepository),
                pgRepository,
                pgApiConfig
        );
    }

    private SqlClient createPgClients(Vertx vertx, PgApiConfig pgApiConfig) {

        if (!pgApiConfig.isEnabled()) {
            return null;
        }

        var databaseConfig = pgApiConfig.getDatabase();
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
