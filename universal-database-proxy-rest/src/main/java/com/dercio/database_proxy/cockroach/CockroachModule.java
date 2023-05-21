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
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import org.apache.commons.lang3.ObjectUtils;

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
            @Nullable @Named("cr.connection.options") PgConnectOptions connectOptions,
            @Named("cr.pool.options") PoolOptions poolOptions
    ) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        return PgPool.pool(vertx, connectOptions, poolOptions);
    }

    @Inject
    @Provides
    @Named("cr.connection.options")
    PgConnectOptions providesConnectionOptions(CrApiConfig crApiConfig,
                                               @Named("system.env.variables") Map<String, String> envVariables) {
        if (!crApiConfig.isEnabled()) {
            return null;
        }

        var databaseConfig = crApiConfig.getDatabase();
        var host = envVariables.getOrDefault(databaseConfig.getHost(), databaseConfig.getHost());
        var username = envVariables.getOrDefault(databaseConfig.getUsername(), databaseConfig.getUsername());
        var password = envVariables.getOrDefault(databaseConfig.getPassword(), databaseConfig.getPassword());
        var databaseName = envVariables.getOrDefault(databaseConfig.getDatabaseName(), databaseConfig.getDatabaseName());
        var pemTrustOptions = new PemTrustOptions();

        if (databaseConfig.isSslEnabled()) {
            ObjectUtils.requireNonEmpty(databaseConfig.getSslCertPath(),
                    "To create an ssl connection with the Postgres Server a valid ssl certificate needs to be provided");
            pemTrustOptions.addCertPath(databaseConfig.getSslCertPath());
        }

        return new PgConnectOptions()
                .setSsl(databaseConfig.isSslEnabled())
                .setPemTrustOptions(pemTrustOptions)
                .setPort(databaseConfig.getPort())
                .setHost(host)
                .setDatabase(databaseName)
                .setUser(username)
                .setPassword(password);
    }

    @Provides
    @Named("cr.pool.options")
    PoolOptions providesPoolOptions() {
        return new PoolOptions().setMaxSize(5);
    }
}
