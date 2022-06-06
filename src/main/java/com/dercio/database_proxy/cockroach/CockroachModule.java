package com.dercio.database_proxy.cockroach;

import com.dercio.database_proxy.common.database.TableRequest;
import com.dercio.database_proxy.common.handlers.FailureHandler;
import com.dercio.database_proxy.common.handlers.NotFoundHandler;
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

import java.util.Map;
import java.util.stream.Collectors;

@Module
public class CockroachModule extends AbstractModule {

    @ProvidesIntoSet
    AbstractVerticle crbApiVerticle(
            FailureHandler failureHandler,
            NotFoundHandler notFoundHandler,
            Vertx vertx,
            CrbApiConfig apiConfig
    ) {

        var pgRepository = new PgRepository(createCrbClients(vertx, apiConfig));

        return new RestApiVerticle(
                failureHandler,
                notFoundHandler,
                new RestApiHandler(pgRepository),
                pgRepository,
                apiConfig
        );
    }

    private Map<String, SqlClient> createCrbClients(Vertx vertx, CrbApiConfig apiConfig) {
        var databaseConfig = apiConfig.getDatabase();
        var password = System.getenv()
                .getOrDefault(databaseConfig.getPassword(), databaseConfig.getPassword());

        return databaseConfig.getTables()
                .stream()
                .map(TableRequest::getDatabase)
                .distinct()
                .map(database -> {
                    PgConnectOptions connectOptions = new PgConnectOptions()
                            .setPort(databaseConfig.getPort())
                            .setHost(databaseConfig.getHost())
                            .setDatabase(database)
                            .setUser(databaseConfig.getUsername())
                            .setPassword(password);

                    PoolOptions poolOptions = new PoolOptions()
                            .setMaxSize(5);

                    return Map.entry(database, PgPool.pool(vertx, connectOptions, poolOptions));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
