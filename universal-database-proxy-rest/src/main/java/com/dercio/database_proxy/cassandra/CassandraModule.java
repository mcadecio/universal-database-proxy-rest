package com.dercio.database_proxy.cassandra;

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
import io.vertx.cassandra.CassandraClient;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

import javax.annotation.Nullable;
import java.util.Map;

@GuiceModule
public class CassandraModule extends AbstractModule {

    @Inject
    @ProvidesIntoSet
    AbstractVerticle cassandraApiVerticle(
            RouterFactory routerFactory,
            CassandraApiConfig apiConfig,
            @Named("cassandra.rest.api.handler") RestApiHandler restApiHandler,
            @Named("cassandra.repository") Repository repository,
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
    @Named("cassandra.rest.api.handler")
    RestApiHandler providesRestApiHandler(@Named("cassandra.repository") Repository repository, Mapper mapper) {
        return new RestApiHandler(mapper, repository);
    }

    @Inject
    @Provides
    @Singleton
    @Named("cassandra.repository")
    Repository providesRepository(
            @Nullable @Named("cassandra.client") CassandraClient cassandraClient,
            CassandraApiConfig apiConfig
    ) {
        var finder = new CassandraObjectFinder(cassandraClient, apiConfig.isAllowFiltering());

        return new CassandraRepository(
                new CassandraObjectDeleter(cassandraClient, finder),
                new CassandraObjectInserter(cassandraClient, finder),
                finder,
                new CassandraTableFinder(cassandraClient)
        );
    }

    @Inject
    @Provides
    @Singleton
    @Named("cassandra.client")
    CassandraClient createCassandraClient(
            Vertx vertx,
            CassandraApiConfig apiConfig,
            @Nullable @Named("cassandra.client.options") CassandraClientOptions clientOptions
    ) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        return CassandraClient.createShared(vertx, "cassandra.api", clientOptions);
    }

    @Inject
    @Provides
    @Named("cassandra.client.options")
    CassandraClientOptions providesClientOptions(
            CassandraApiConfig apiConfig,
            @Named("system.env.variables") Map<String, String> envVariables
    ) {
        if (!apiConfig.isEnabled()) {
            return null;
        }

        return new CassandraClientOptionsFactory().create(apiConfig.getDatabase(), envVariables);
    }
}
