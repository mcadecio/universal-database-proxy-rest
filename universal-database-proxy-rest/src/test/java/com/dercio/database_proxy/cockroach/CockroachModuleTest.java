package com.dercio.database_proxy.cockroach;

import com.dercio.database_proxy.common.database.DatabaseConfig;
import com.google.inject.*;
import com.google.inject.name.Names;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CockroachModuleTest {
    private final Injector injector = Guice.createInjector(new CockroachModule(), createModuleSetup(createCrApiConfig()));

    @Test
    void shouldConfigureCrConnectOptionsBasedOnEnvVariables() {
        var pgConnectOptions = injector.getInstance(Key.get(PgConnectOptions.class, Names.named("cr.connection.options")));

        assertAll(
                () -> assertEquals(2020, pgConnectOptions.getPort()),
                () -> assertEquals("localhost", pgConnectOptions.getHost()),
                () -> assertEquals("default", pgConnectOptions.getDatabase()),
                () -> assertEquals("root", pgConnectOptions.getUser()),
                () -> assertEquals("pass", pgConnectOptions.getPassword())
        );
    }

    @Test
    void shouldConfigureCrConnectOptionsUsingProvidedProperties() {
        var crApiConfig = injector.getInstance(CrApiConfig.class).getDatabase();
        crApiConfig.setDatabaseName("database");
        crApiConfig.setHost("192.0.0.1");
        crApiConfig.setUsername("main");
        crApiConfig.setPassword("1234");

        var pgConnectOptions = injector.getInstance(Key.get(PgConnectOptions.class, Names.named("cr.connection.options")));

        assertAll(
                () -> assertEquals(2020, pgConnectOptions.getPort()),
                () -> assertEquals("192.0.0.1", pgConnectOptions.getHost()),
                () -> assertEquals("database", pgConnectOptions.getDatabase()),
                () -> assertEquals("main", pgConnectOptions.getUser()),
                () -> assertEquals("1234", pgConnectOptions.getPassword())
        );
    }

    private CrApiConfig createCrApiConfig() {
        CrApiConfig crApiConfig = new CrApiConfig();
        crApiConfig.setEnabled(true);
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setHost("CR_HOST");
        databaseConfig.setUsername("CR_USER");
        databaseConfig.setPassword("CR_PASS");
        databaseConfig.setDatabaseName("CR_DBNAME");
        databaseConfig.setPort(2020);
        crApiConfig.setDatabase(databaseConfig);
        return crApiConfig;
    }

    private AbstractModule createModuleSetup(CrApiConfig crApiConfig) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                var envVariables = Map.of(
                        "CR_HOST", "localhost",
                        "CR_USER", "root",
                        "CR_PASS", "pass",
                        "CR_DBNAME", "default"
                );

                bind(new TypeLiteral<Map<String, String>>() {
                })
                        .annotatedWith(Names.named("system.env.variables"))
                        .toInstance(envVariables);
                bind(Clock.class).toInstance(Clock.systemDefaultZone());
                bind(CrApiConfig.class).toInstance(crApiConfig);
                bind(Vertx.class).toInstance(Vertx.vertx());
            }
        };
    }
}