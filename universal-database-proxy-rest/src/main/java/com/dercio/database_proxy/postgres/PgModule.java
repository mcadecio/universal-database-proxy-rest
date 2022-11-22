package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.handlers.FailureHandler;
import com.dercio.database_proxy.common.handlers.NotFoundHandler;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.common.module.GuiceModule;
import com.dercio.database_proxy.restapi.RestApiHandler;
import com.dercio.database_proxy.restapi.RestApiVerticle;
import com.google.inject.*;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;

@GuiceModule
public class PgModule extends AbstractModule {

    @ProvidesIntoSet
    AbstractVerticle pgApiVerticle(
            FailureHandler failureHandler,
            NotFoundHandler notFoundHandler,
            Vertx vertx,
            PgApiConfig apiConfig,
            Mapper mapper
    ) {

        var sqlClient = createSqlClient(vertx, apiConfig);
        var pgRepository = pgRepository(sqlClient);

        return new RestApiVerticle(
                failureHandler,
                notFoundHandler,
                new RestApiHandler(mapper, pgRepository),
                pgRepository,
                apiConfig
        );
    }

    Repository pgRepository(SqlClient sqlClient) {
        return new PgRepository(
                new PgObjectDeleter(sqlClient),
                new PgObjectInserter(sqlClient),
                new PgObjectFinder(sqlClient),
                new PgTableFinder(sqlClient)
        );
    }

    private SqlClient createSqlClient(Vertx vertx, ApiConfig apiConfig) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        PgConnectOptions connectOptions = pgConnectOptions(apiConfig);
        PoolOptions poolOptions = poolOptions();

        return PgPool.pool(vertx, connectOptions, poolOptions);
    }

    PgConnectOptions pgConnectOptions(ApiConfig pgApiConfig) {
        var databaseConfig = pgApiConfig.getDatabase();
        var password = System.getenv()
                .getOrDefault(databaseConfig.getPassword(), databaseConfig.getPassword());
        return new PgConnectOptions()
                .setPort(databaseConfig.getPort())
                .setHost(databaseConfig.getHost())
                .setDatabase(databaseConfig.getDatabaseName())
                .setUser(databaseConfig.getUsername())
                .setPassword(password);
    }

    PoolOptions poolOptions() {
        return new PoolOptions().setMaxSize(5);
    }
}
