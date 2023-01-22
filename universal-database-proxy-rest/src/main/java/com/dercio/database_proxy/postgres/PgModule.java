package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.common.module.GuiceModule;
import com.dercio.database_proxy.common.router.RouterFactory;
import com.dercio.database_proxy.restapi.RestApiHandler;
import com.dercio.database_proxy.restapi.RestApiVerticleSelector;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
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
            RouterFactory routerFactory,
            PgApiConfig apiConfig,
            @Named("pg.rest.api.handler") RestApiHandler restApiHandler,
            @Named("pg.repository") Repository repository,
            RestApiVerticleSelector restApiVerticleSelector
    ) {

        return restApiVerticleSelector.select(
                routerFactory,
                restApiHandler,
                repository,
                apiConfig
        );
    }

    @Provides
    @Named("pg.rest.api.handler")
    RestApiHandler providesPgRestApiHandler(@Named("pg.repository") Repository repository, Mapper mapper) {
        return new RestApiHandler(mapper, repository);
    }

    @Provides
    @Singleton
    @Named("pg.repository")
    Repository pgRepository(@Named("pg.sql.client") SqlClient sqlClient) {

        return new PgRepository(
                new PgObjectDeleter(sqlClient),
                new PgObjectInserter(sqlClient),
                new PgObjectFinder(sqlClient),
                new PgTableFinder(sqlClient)
        );
    }

    @Provides
    @Named("pg.sql.client")
    SqlClient createSqlClient(
            Vertx vertx,
            PgApiConfig apiConfig,
            @Named("pg.connection.options") PgConnectOptions connectOptions,
            @Named("pg.pool.options") PoolOptions poolOptions
    ) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        return PgPool.pool(vertx, connectOptions, poolOptions);
    }

    @Provides
    @Named("pg.connection.options")
    PgConnectOptions pgConnectOptions(PgApiConfig pgApiConfig) {
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

    @Provides
    @Named("pg.pool.options")
    PoolOptions poolOptions() {
        return new PoolOptions().setMaxSize(5);
    }
}
