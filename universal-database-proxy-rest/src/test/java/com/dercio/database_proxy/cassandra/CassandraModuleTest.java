package com.dercio.database_proxy.cassandra;

import com.dercio.database_proxy.common.database.DatabaseConfig;
import com.google.inject.*;
import com.google.inject.name.Names;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CassandraModuleTest {

    private final Injector injector =
            Guice.createInjector(new CassandraModule(), createModuleSetup(createCassandraApiConfig()));

    @Test
    void shouldConfigureCassandraClientOptionsBasedOnEnvVariables() {
        var options = clientOptions();

        assertAll(
                () -> assertEquals("music", options.getKeyspace()),
                () -> assertEquals("root", options.getUsername()),
                () -> assertEquals("pass", options.getPassword())
        );
    }

    @Test
    void shouldConfigureCassandraClientOptionsUsingProvidedProperties() {
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setDatabaseName("catalogue");
        databaseConfig.setUsername("admin");
        databaseConfig.setPassword("1234");

        var options = clientOptions();

        assertAll(
                () -> assertEquals("catalogue", options.getKeyspace()),
                () -> assertEquals("admin", options.getUsername()),
                () -> assertEquals("1234", options.getPassword())
        );
    }

    @Test
    void shouldLeaveCredentialsUnsetWhenNoUsernameIsConfigured() {
        // Cassandra runs without authentication by default and empty credentials fail the handshake.
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setUsername("");

        var options = clientOptions();

        assertAll(
                () -> assertNull(options.getUsername()),
                () -> assertNull(options.getPassword())
        );
    }

    @Test
    void shouldDefaultAllowFilteringToFalseSoAScanIsNeverImplicit() {
        assertFalse(injector.getInstance(CassandraApiConfig.class).isAllowFiltering());
    }

    @Test
    void shouldFailWhenNoLocalDatacenterIsConfigured() {
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setLocalDatacenter(null);

        var exception = assertThrows(ProvisionException.class, this::clientOptions);

        assertTrue(exception.getMessage().contains("localDatacenter is required"));
    }

    @Test
    void shouldUseEveryConfiguredHostAsAContactPoint() {
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setHosts(List.of("cassandra-1:9042", "cassandra-2:9042"));

        assertDoesNotThrow(this::clientOptions);
    }

    private CassandraClientOptions clientOptions() {
        return injector.getInstance(Key.get(CassandraClientOptions.class, Names.named("cassandra.client.options")));
    }

    private CassandraApiConfig createCassandraApiConfig() {
        var apiConfig = new CassandraApiConfig();
        apiConfig.setEnabled(true);

        var databaseConfig = new DatabaseConfig();
        databaseConfig.setHost("CASSANDRA_HOST");
        databaseConfig.setUsername("CASSANDRA_USER");
        databaseConfig.setPassword("CASSANDRA_PASS");
        databaseConfig.setDatabaseName("CASSANDRA_KEYSPACE");
        databaseConfig.setPort(9042);
        databaseConfig.setLocalDatacenter("datacenter1");
        apiConfig.setDatabase(databaseConfig);

        return apiConfig;
    }

    private AbstractModule createModuleSetup(CassandraApiConfig cassandraApiConfig) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                var envVariables = Map.of(
                        "CASSANDRA_HOST", "localhost",
                        "CASSANDRA_USER", "root",
                        "CASSANDRA_PASS", "pass",
                        "CASSANDRA_KEYSPACE", "music"
                );

                bind(new TypeLiteral<Map<String, String>>() {
                })
                        .annotatedWith(Names.named("system.env.variables"))
                        .toInstance(envVariables);
                bind(Clock.class).toInstance(Clock.systemDefaultZone());
                bind(CassandraApiConfig.class).toInstance(cassandraApiConfig);
                bind(Vertx.class).toInstance(Vertx.vertx());
            }
        };
    }
}
