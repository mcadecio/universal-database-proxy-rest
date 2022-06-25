package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.TableRequest;
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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

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

    private Map<String, SqlClient> createPgClients(Vertx vertx, PgApiConfig pgApiConfig) {

        if (!pgApiConfig.isEnabled()) {
            return Collections.emptyMap();
        }

        var databaseConfig = pgApiConfig.getDatabase();
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
