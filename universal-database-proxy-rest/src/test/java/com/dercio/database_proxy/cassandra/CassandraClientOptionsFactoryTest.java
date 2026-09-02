package com.dercio.database_proxy.cassandra;

import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.dercio.database_proxy.common.database.DatabaseConfig;
import io.vertx.cassandra.CassandraClientOptions;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CassandraClientOptionsFactoryTest {

    private static final Map<String, String> ENV = Map.of(
            "CASSANDRA_HOSTS", "env-1:9042,env-2:9042",
            "CASSANDRA_USER", "root",
            "CASSANDRA_PASS", "pass",
            "CASSANDRA_KEYSPACE", "music",
            "CASSANDRA_DC", "datacenter1"
    );

    private final CassandraClientOptionsFactory factory = new CassandraClientOptionsFactory();

    @Test
    void shouldResolveConnectionDetailsFromEnvironmentVariables() {
        var apiConfig = config();
        apiConfig.getDatabase().setUsername("CASSANDRA_USER");
        apiConfig.getDatabase().setPassword("CASSANDRA_PASS");
        apiConfig.getDatabase().setDatabaseName("CASSANDRA_KEYSPACE");

        var options = factory.create(apiConfig, ENV);

        assertAll(
                () -> assertEquals("music", options.getKeyspace()),
                () -> assertEquals("root", options.getUsername()),
                () -> assertEquals("pass", options.getPassword())
        );
    }

    @Test
    void shouldFallBackToLiteralValuesWhenNoEnvironmentVariableMatches() {
        var options = factory.create(config(), ENV);

        assertAll(
                () -> assertEquals("catalogue", options.getKeyspace()),
                () -> assertEquals("admin", options.getUsername()),
                () -> assertEquals("1234", options.getPassword())
        );
    }

    @Test
    void shouldLeaveCredentialsUnsetWhenNoUsernameIsConfigured() {
        var apiConfig = config();
        apiConfig.getDatabase().setUsername("");

        var options = factory.create(apiConfig, ENV);

        assertAll(
                () -> assertNull(options.getUsername()),
                () -> assertNull(options.getPassword())
        );
    }

    @Test
    void shouldAddEveryConfiguredHostAsAContactPoint() {
        var apiConfig = config();
        apiConfig.getDatabase().setHosts(List.of("cassandra-1:9042", "cassandra-2:9142"));

        assertEquals(
                Set.of(InetSocketAddress.createUnresolved("cassandra-1", 9042),
                        InetSocketAddress.createUnresolved("cassandra-2", 9142)),
                contactPoints(factory.create(apiConfig, ENV)));
    }

    @Test
    void shouldExpandACommaSeparatedEnvironmentVariableIntoSeveralContactPoints() {
        var apiConfig = config();
        apiConfig.getDatabase().setHosts(List.of("CASSANDRA_HOSTS"));

        assertEquals(
                Set.of(InetSocketAddress.createUnresolved("env-1", 9042),
                        InetSocketAddress.createUnresolved("env-2", 9042)),
                contactPoints(factory.create(apiConfig, ENV)));
    }

    @Test
    void shouldFallBackToHostAndPortWhenNoHostsAreConfigured() {
        var apiConfig = config();
        apiConfig.getDatabase().setHosts(null);
        apiConfig.getDatabase().setHost("legacy-host");
        apiConfig.getDatabase().setPort(9999);

        assertEquals(
                Set.of(InetSocketAddress.createUnresolved("legacy-host", 9999)),
                contactPoints(factory.create(apiConfig, ENV)));
    }

    @Test
    void shouldFailWhenAContactPointIsNotInHostPortForm() {
        var apiConfig = config();
        apiConfig.getDatabase().setHosts(List.of("cassandra-1"));

        var exception = assertThrows(IllegalStateException.class, () -> factory.create(apiConfig, ENV));

        assertTrue(exception.getMessage().contains("must be in host:port form"));
    }

    @Test
    void shouldFailWhenNoHostsAreConfiguredAtAll() {
        var apiConfig = config();
        apiConfig.getDatabase().setHosts(null);
        apiConfig.getDatabase().setHost(null);

        var exception = assertThrows(IllegalStateException.class, () -> factory.create(apiConfig, ENV));

        assertTrue(exception.getMessage().contains("No database hosts configured"));
    }

    @Test
    void shouldFailWhenLocalDatacenterIsMissing() {
        var missing = config();
        missing.setLocalDatacenter(null);

        var blank = config();
        blank.setLocalDatacenter("  ");

        assertAll(
                () -> assertTrue(assertThrows(IllegalStateException.class, () -> factory.create(missing, ENV))
                        .getMessage().contains("localDatacenter is required")),
                () -> assertTrue(assertThrows(IllegalStateException.class, () -> factory.create(blank, ENV))
                        .getMessage().contains("localDatacenter is required"))
        );
    }

    @Test
    void shouldResolveLocalDatacenterFromAnEnvironmentVariable() {
        var apiConfig = config();
        apiConfig.setLocalDatacenter("CASSANDRA_DC");

        assertTrue(localDatacenterConfigured(factory.create(apiConfig, ENV)));
    }

    @Test
    void shouldNotConfigureSslWhenDisabled() {
        var apiConfig = config();
        apiConfig.getDatabase().setSslEnabled(false);
        apiConfig.getDatabase().setSslCertPath("/does/not/exist.pem");

        assertFalse(sslConfigured(factory.create(apiConfig, ENV)),
                "A bogus certificate path must not even be read while ssl is disabled");
    }

    @Test
    void shouldConfigureSslWhenEnabled() {
        var apiConfig = config();
        apiConfig.getDatabase().setSslEnabled(true);
        apiConfig.getDatabase().setSslCertPath(certificate("cassandra-test-cert.pem"));

        assertTrue(sslConfigured(factory.create(apiConfig, ENV)));
    }

    @Test
    void shouldResolveTheCertificatePathFromAnEnvironmentVariable() {
        var apiConfig = config();
        apiConfig.getDatabase().setSslEnabled(true);
        apiConfig.getDatabase().setSslCertPath("CASSANDRA_CERT");

        var env = new HashMap<>(ENV);
        env.put("CASSANDRA_CERT", certificate("cassandra-test-cert.pem"));

        assertTrue(sslConfigured(factory.create(apiConfig, env)));
    }

    @Test
    void shouldFailWhenSslIsEnabledWithoutACertificate() {
        var apiConfig = config();
        apiConfig.getDatabase().setSslEnabled(true);

        var exception = assertThrows(IllegalStateException.class, () -> factory.create(apiConfig, ENV));

        assertTrue(exception.getMessage().contains("sslCertPath is required"));
    }

    @Test
    void shouldFailWhenTheCertificateCannotBeRead() {
        var apiConfig = config();
        apiConfig.getDatabase().setSslEnabled(true);
        apiConfig.getDatabase().setSslCertPath("/does/not/exist.pem");

        var exception = assertThrows(IllegalStateException.class, () -> factory.create(apiConfig, ENV));

        assertTrue(exception.getMessage().contains("/does/not/exist.pem"));
    }

    @Test
    void shouldFailWhenTheCertificateIsNotValidPem() {
        var apiConfig = config();
        apiConfig.getDatabase().setSslEnabled(true);
        apiConfig.getDatabase().setSslCertPath(certificate("not-a-certificate.pem"));

        var exception = assertThrows(IllegalStateException.class, () -> factory.create(apiConfig, ENV));

        assertTrue(exception.getMessage().contains("Could not read the PEM encoded ssl certificate"));
    }

    @Test
    void shouldTrustOnlyTheConfiguredCertificate() {
        var issuers = acceptedIssuers(certificate("cassandra-test-cert.pem"));

        assertEquals(List.of("CN=cassandra-test"), issuers);
    }

    @Test
    void shouldNotTrustAnUnrelatedCertificate() {
        var issuers = acceptedIssuers(certificate("unrelated-cert.pem"));

        assertAll(
                () -> assertEquals(List.of("CN=unrelated-ca"), issuers),
                () -> assertFalse(issuers.contains("CN=cassandra-test"))
        );
    }

    private List<String> acceptedIssuers(String certPath) {
        var trustManager = (X509TrustManager) CassandraClientOptionsFactory.trustManagers(certPath)[0];

        return Arrays.stream(trustManager.getAcceptedIssuers())
                .map(certificate -> certificate.getSubjectX500Principal().getName())
                .toList();
    }

    private CassandraApiConfig config() {
        var databaseConfig = new DatabaseConfig();
        databaseConfig.setHosts(List.of("localhost:9042"));
        databaseConfig.setUsername("admin");
        databaseConfig.setPassword("1234");
        databaseConfig.setDatabaseName("catalogue");

        var apiConfig = new CassandraApiConfig();
        apiConfig.setLocalDatacenter("datacenter1");
        apiConfig.setDatabase(databaseConfig);

        return apiConfig;
    }

    @SneakyThrows
    private String certificate(String name) {
        return Paths.get(getClass().getClassLoader().getResource(name).toURI()).toString();
    }

    /**
     * The driver keeps contact points and the ssl/datacenter flags on the session builder with no
     * public accessor, so these reach for the fields. A driver upgrade that renames them should fail
     * loudly here rather than silently stop asserting anything.
     */
    @SuppressWarnings("unchecked")
    private Set<SocketAddress> contactPoints(CassandraClientOptions options) {
        var endPoints = (Set<EndPoint>) builderField(options, "programmaticContactPoints");

        return endPoints.stream().map(EndPoint::resolve).collect(Collectors.toSet());
    }

    private boolean sslConfigured(CassandraClientOptions options) {
        return (boolean) builderField(options, "programmaticSslFactory");
    }

    private boolean localDatacenterConfigured(CassandraClientOptions options) {
        return (boolean) builderField(options, "programmaticLocalDatacenter");
    }

    @SneakyThrows
    private Object builderField(CassandraClientOptions options, String name) {
        var builder = options.dataStaxClusterBuilder();
        var field = builder.getClass().getSuperclass().getDeclaredField(name);
        field.setAccessible(true);

        return field.get(builder);
    }
}
