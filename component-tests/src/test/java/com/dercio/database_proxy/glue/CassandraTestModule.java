package com.dercio.database_proxy.glue;

import com.datastax.oss.driver.api.core.CqlSession;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import org.apache.ibatis.io.Resources;

import java.net.InetSocketAddress;

public class CassandraTestModule extends AbstractModule {
    @Provides
    @Singleton
    @SneakyThrows
    CqlSession cqlSession() {
        var properties = Resources.getResourceAsProperties("cassandra/cassandra.properties");

        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(
                        properties.getProperty("contactPoint"),
                        Integer.parseInt(properties.getProperty("port"))))
                .withLocalDatacenter(properties.getProperty("localDatacenter"))
                .withKeyspace(properties.getProperty("keyspace"))
                .build();
    }
}
