package com.dercio.database_proxy.cockroach;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.common.module.GuiceModule;
import com.dercio.database_proxy.common.router.RouterFactory;
import com.dercio.database_proxy.postgres.*;
import com.dercio.database_proxy.restapi.RestApiHandler;
import com.dercio.database_proxy.restapi.RestApiVerticleSelector;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;

import javax.annotation.Nullable;
import java.util.Map;

@GuiceModule
public class CockroachModule extends AbstractModule {

    @Inject
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

    @Inject
    @Provides
    @Named("cr.rest.api.handler")
    RestApiHandler providesRestApiHandler(@Named("cr.repository") Repository repository, Mapper mapper) {
        return new RestApiHandler(mapper, repository);
    }

    @Inject
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

    @Inject
    @Provides
    @Named("cr.sql.client")
    SqlClient createSqlClient(
            Vertx vertx,
            CrApiConfig apiConfig,
            @Nullable @Named("cr.connection.options") JDBCConnectOptions connectOptions,
            @Named("cr.pool.options") PoolOptions poolOptions
    ) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        return JDBCPool.pool(vertx, connectOptions, poolOptions);
    }

    @Inject
    @Provides
    @Named("cr.connection.options")
    JDBCConnectOptions providesConnectionOptions(CrApiConfig crApiConfig,
                                                 @Named("system.env.variables") Map<String, String> envVariables) {
        if (!crApiConfig.isEnabled()) {
            return null;
        }

        var databaseConfig = crApiConfig.getDatabase();

        return new PgJdbcConnectOptionsFactory().create(databaseConfig, envVariables);
    }

    @Provides
    @Named("cr.pool.options")
    PoolOptions providesPoolOptions() {
        return new PoolOptions().setMaxSize(5).setName("cr.pool");
    }
}
