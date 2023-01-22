package com.dercio.database_proxy.cockroach;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.common.module.GuiceModule;
import com.dercio.database_proxy.common.router.RouterFactory;
import com.dercio.database_proxy.postgres.PgObjectDeleter;
import com.dercio.database_proxy.postgres.PgObjectFinder;
import com.dercio.database_proxy.postgres.PgObjectInserter;
import com.dercio.database_proxy.postgres.PgRepository;
import com.dercio.database_proxy.postgres.PgTableFinder;
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

@GuiceModule
public class CockroachModule extends AbstractModule {

    @ProvidesIntoSet
    AbstractVerticle crApiVerticle(
            RouterFactory routerFactory,
            CrApiConfig apiConfig,
            @Named("cr.rest.api.handler") RestApiHandler restApiHandler,
            @Named("cr.repository") Repository repository,
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
    @Named("cr.rest.api.handler")
    RestApiHandler providesRestApiHandler(@Named("cr.repository") Repository repository, Mapper mapper) {
        return new RestApiHandler(mapper, repository);
    }

    @Provides
    @Singleton
    @Named("cr.repository")
    Repository providesRepository(@Nullable @Named("cr.sql.client") SqlClient sqlClient) {

        return new PgRepository(
                new PgObjectDeleter(sqlClient),
                new PgObjectInserter(sqlClient),
                new PgObjectFinder(sqlClient),
                new PgTableFinder(sqlClient)
        );
    }

    @Provides
    @Named("cr.sql.client")
    SqlClient createSqlClient(
            Vertx vertx,
            CrApiConfig apiConfig,
            @Nullable @Named("cr.connection.options") PgConnectOptions connectOptions,
            @Named("cr.pool.options") PoolOptions poolOptions
    ) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        return PgPool.pool(vertx, connectOptions, poolOptions);
    }

    @Provides
    @Named("cr.connection.options")
    PgConnectOptions providesConnectionOptions(CrApiConfig crApiConfig) {
        if (!crApiConfig.isEnabled()) {
            return null;
        }

        var databaseConfig = crApiConfig.getDatabase();
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
    @Named("cr.pool.options")
    PoolOptions providesPoolOptions() {
        return new PoolOptions().setMaxSize(5);
    }
}
