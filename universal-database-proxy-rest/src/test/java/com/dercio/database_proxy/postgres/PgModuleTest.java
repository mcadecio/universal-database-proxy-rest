package com.dercio.database_proxy.postgres;

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

class PgModuleTest {

    private final Injector injector = Guice.createInjector(new PgModule(), createModuleSetup(createPgApiConfig()));

    @Test
    void shouldConfigurePgConnectOptionsBasedOnEnvVariables() {
        var pgConnectOptions = injector.getInstance(Key.get(PgConnectOptions.class, Names.named("pg.connection.options")));

        assertAll(
                () -> assertEquals(2020, pgConnectOptions.getPort()),
                () -> assertEquals("localhost", pgConnectOptions.getHost()),
                () -> assertEquals("default", pgConnectOptions.getDatabase()),
                () -> assertEquals("root", pgConnectOptions.getUser()),
                () -> assertEquals("pass", pgConnectOptions.getPassword())
        );
    }

    @Test
    void shouldConfigurePgConnectOptionsUsingProvidedProperties() {
        var pgApiConfig = injector.getInstance(PgApiConfig.class).getDatabase();
        pgApiConfig.setDatabaseName("database");
        pgApiConfig.setHost("192.0.0.1");
        pgApiConfig.setUsername("main");
        pgApiConfig.setPassword("1234");

        var pgConnectOptions = injector.getInstance(Key.get(PgConnectOptions.class, Names.named("pg.connection.options")));

        assertAll(
                () -> assertEquals(2020, pgConnectOptions.getPort()),
                () -> assertEquals("192.0.0.1", pgConnectOptions.getHost()),
                () -> assertEquals("database", pgConnectOptions.getDatabase()),
                () -> assertEquals("main", pgConnectOptions.getUser()),
                () -> assertEquals("1234", pgConnectOptions.getPassword())
        );
    }

    private PgApiConfig createPgApiConfig() {
        PgApiConfig pgApiConfig = new PgApiConfig();
        pgApiConfig.setEnabled(true);
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setHost("PG_HOST");
        databaseConfig.setUsername("PG_USER");
        databaseConfig.setPassword("PG_PASS");
        databaseConfig.setDatabaseName("PG_DBNAME");
        databaseConfig.setPort(2020);
        pgApiConfig.setDatabase(databaseConfig);
        return pgApiConfig;
    }

    private AbstractModule createModuleSetup(PgApiConfig pgApiConfig) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                var envVariables = Map.of(
                        "PG_HOST", "localhost",
                        "PG_USER", "root",
                        "PG_PASS", "pass",
                        "PG_DBNAME", "default"
                );

                bind(new TypeLiteral<Map<String, String>>() {
                })
                        .annotatedWith(Names.named("system.env.variables"))
                        .toInstance(envVariables);
                bind(Clock.class).toInstance(Clock.systemDefaultZone());
                bind(PgApiConfig.class).toInstance(pgApiConfig);
                bind(Vertx.class).toInstance(Vertx.vertx());
            }
        };
    }

}