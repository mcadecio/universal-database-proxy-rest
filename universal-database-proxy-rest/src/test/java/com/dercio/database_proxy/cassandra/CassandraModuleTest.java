package com.dercio.database_proxy.cassandra;

import com.dercio.database_proxy.common.database.DatabaseConfig;
import com.google.inject.*;
import com.google.inject.name.Names;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.net.ssl.X509TrustManager;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Arrays;
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
        injector.getInstance(CassandraApiConfig.class).setLocalDatacenter(null);

        var exception = assertThrows(ProvisionException.class, this::clientOptions);

        assertTrue(exception.getMessage().contains("localDatacenter is required"));
    }

    @Test
    void shouldUseEveryConfiguredHostAsAContactPoint() {
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setHosts(List.of("cassandra-1:9042", "cassandra-2:9042"));

        assertDoesNotThrow(this::clientOptions);
    }

    @Test
    void shouldFailWhenSslIsEnabledWithoutACertificate() {
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setSslEnabled(true);

        var exception = assertThrows(ProvisionException.class, this::clientOptions);

        assertTrue(exception.getMessage().contains("sslCertPath is required"));
    }

    @Test
    void shouldFailWhenTheConfiguredCertificateCannotBeRead() {
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setSslEnabled(true);
        databaseConfig.setSslCertPath("/does/not/exist.pem");

        var exception = assertThrows(ProvisionException.class, this::clientOptions);

        assertTrue(exception.getMessage().contains("/does/not/exist.pem"));
    }

    @Test
    void shouldBuildOptionsWhenSslIsEnabledWithAValidCertificate() {
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setSslEnabled(true);
        databaseConfig.setSslCertPath(certificatePath());

        assertDoesNotThrow(this::clientOptions);
    }

    @Test
    void shouldNotReadTheCertificateWhenSslIsDisabled() {
        var databaseConfig = injector.getInstance(CassandraApiConfig.class).getDatabase();
        databaseConfig.setSslEnabled(false);
        databaseConfig.setSslCertPath("/does/not/exist.pem");

        assertDoesNotThrow(this::clientOptions);
    }

    @Test
    void shouldTrustOnlyTheConfiguredCertificate() {
        var trustManager = (X509TrustManager) CassandraClientOptionsFactory.trustManagers(certificatePath())[0];

        var issuers = Arrays.stream(trustManager.getAcceptedIssuers())
                .map(certificate -> certificate.getSubjectX500Principal().getName())
                .toList();

        assertEquals(List.of("CN=cassandra-test"), issuers);
    }

    @SneakyThrows
    private String certificatePath() {
        return Paths.get(getClass().getClassLoader().getResource("cassandra-test-cert.pem").toURI()).toString();
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
        apiConfig.setDatabase(databaseConfig);
        apiConfig.setLocalDatacenter("datacenter1");

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
