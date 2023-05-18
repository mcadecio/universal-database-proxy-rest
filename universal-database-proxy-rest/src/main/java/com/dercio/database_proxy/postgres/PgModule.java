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

import javax.annotation.Nullable;
import java.util.Map;

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
    Repository pgRepository(@Nullable @Named("pg.sql.client") SqlClient sqlClient) {

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
            @Nullable @Named("pg.connection.options") PgConnectOptions connectOptions,
            @Named("pg.pool.options") PoolOptions poolOptions
    ) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        return PgPool.pool(vertx, connectOptions, poolOptions);
    }

    @Provides
    @Named("pg.connection.options")
    PgConnectOptions pgConnectOptions(PgApiConfig pgApiConfig,
                                      @Named("system.env.variables") Map<String, String> envVariables) {
        if (!pgApiConfig.isEnabled()) {
            return null;
        }

        var databaseConfig = pgApiConfig.getDatabase();
        var host = envVariables.getOrDefault(databaseConfig.getHost(), databaseConfig.getHost());
        var username = envVariables.getOrDefault(databaseConfig.getUsername(), databaseConfig.getUsername());
        var password = envVariables.getOrDefault(databaseConfig.getPassword(), databaseConfig.getPassword());
        var databaseName = envVariables.getOrDefault(databaseConfig.getDatabaseName(), databaseConfig.getDatabaseName());

        return new PgConnectOptions()
                .setPort(databaseConfig.getPort())
                .setHost(host)
                .setDatabase(databaseName)
                .setUser(username)
                .setPassword(password);
    }

    @Provides
    @Named("pg.pool.options")
    PoolOptions poolOptions() {
        return new PoolOptions().setMaxSize(5);
    }
}
