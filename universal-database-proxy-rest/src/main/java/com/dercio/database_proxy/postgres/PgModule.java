package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.dercio.database_proxy.common.module.GuiceModule;
import com.dercio.database_proxy.common.router.RouterFactory;
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
public class PgModule extends AbstractModule {

    @Inject
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

    @Inject
    @Provides
    @Named("pg.rest.api.handler")
    RestApiHandler providesPgRestApiHandler(@Named("pg.repository") Repository repository, Mapper mapper) {
        return new RestApiHandler(mapper, repository);
    }

    @Inject
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

    @Inject
    @Provides
    @Named("pg.sql.client")
    SqlClient createSqlClient(
            Vertx vertx,
            PgApiConfig apiConfig,
            @Nullable @Named("pg.connection.options") JDBCConnectOptions connectOptions,
            @Named("pg.pool.options") PoolOptions poolOptions
    ) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        return JDBCPool.pool(vertx, connectOptions, poolOptions);
    }

    @Inject
    @Provides
    @Named("pg.connection.options")
    JDBCConnectOptions connectOptions(PgApiConfig pgApiConfig,
                                      @Named("system.env.variables") Map<String, String> envVariables) {
        if (!pgApiConfig.isEnabled()) {
            return null;
        }
        var databaseConfig = pgApiConfig.getDatabase();
        return new PgJdbcConnectOptionsFactory().create(databaseConfig, envVariables);
    }

    @Provides
    @Named("pg.pool.options")
    PoolOptions poolOptions() {
        return new PoolOptions().setMaxSize(5).setName("pg.pool");
    }
}
