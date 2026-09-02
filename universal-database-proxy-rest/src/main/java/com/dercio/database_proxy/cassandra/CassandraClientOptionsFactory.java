package com.dercio.database_proxy.cassandra;

import com.dercio.database_proxy.common.database.ConfigValues;
import com.dercio.database_proxy.common.database.DatabaseConfig;
import io.vertx.cassandra.CassandraClientOptions;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Map;

public class CassandraClientOptionsFactory {

    public CassandraClientOptions create(CassandraApiConfig apiConfig, Map<String, String> envVariables) {
        var databaseConfig = apiConfig.getDatabase();
        var username = ConfigValues.resolve(databaseConfig.getUsername(), envVariables);
        var password = ConfigValues.resolve(databaseConfig.getPassword(), envVariables);
        var keyspace = ConfigValues.resolve(databaseConfig.getDatabaseName(), envVariables);
        var localDatacenter = resolveLocalDatacenter(apiConfig, envVariables);

        var options = new CassandraClientOptions().setKeyspace(keyspace);

        ConfigValues.resolveHosts(databaseConfig, envVariables)
                .forEach(contactPoint -> addContactPoint(options, contactPoint));

        if (isNotBlank(username)) {
            options.setUsername(username).setPassword(password);
        }

        options.dataStaxClusterBuilder().withLocalDatacenter(localDatacenter);

        if (databaseConfig.isSslEnabled()) {
            options.dataStaxClusterBuilder().withSslContext(sslContext(databaseConfig, envVariables));
        }

        return options;
    }

    private SSLContext sslContext(DatabaseConfig databaseConfig, Map<String, String> envVariables) {
        var sslCertPath = ConfigValues.resolve(databaseConfig.getSslCertPath(), envVariables);

        if (!isNotBlank(sslCertPath)) {
            throw new IllegalStateException(
                    "cassandraApi.database.sslCertPath is required when sslEnabled is true: provide the "
                            + "PEM encoded CA certificate the cluster's certificate was signed with.");
        }

        try {
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers(sslCertPath), null);

            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not build an ssl context for [" + sslCertPath + "]", e);
        }
    }

    static TrustManager[] trustManagers(String sslCertPath) {
        try (var certificate = Files.newInputStream(Path.of(sslCertPath))) {
            var trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("default",
                    CertificateFactory.getInstance("X.509").generateCertificate(certificate));

            var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            return trustManagerFactory.getTrustManagers();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not read the PEM encoded ssl certificate at [" + sslCertPath + "]", e);
        }
    }

    private void addContactPoint(CassandraClientOptions options, String contactPoint) {
        var separator = contactPoint.lastIndexOf(':');

        if (separator < 1 || separator == contactPoint.length() - 1) {
            throw new IllegalStateException(
                    "Cassandra contact point [" + contactPoint + "] must be in host:port form");
        }

        options.addContactPoint(
                contactPoint.substring(0, separator),
                Integer.parseInt(contactPoint.substring(separator + 1))
        );
    }

    private String resolveLocalDatacenter(CassandraApiConfig apiConfig, Map<String, String> envVariables) {
        var configured = ConfigValues.resolve(apiConfig.getLocalDatacenter(), envVariables);

        if (isNotBlank(configured)) {
            return configured;
        }

        throw new IllegalStateException(
                "cassandraApi.localDatacenter is required: the DataStax driver cannot route "
                        + "without it.");
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
