package com.dercio.database_proxy.proxy;

import com.dercio.database_proxy.common.verticle.Verticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Verticle
public class ProxyVerticle extends AbstractVerticle {

    private NetServer netServer;

    @Override
    public void start(Promise<Void> startPromise) {
        this.netServer = vertx.createNetServer();

        netServer.connectHandler(socket -> {
            log.info("Client connected to Proxy. Client: {}", socket.remoteAddress());
            AtomicReference<Future<NetSocket>> connectionReference = new AtomicReference<>();
            socket.handler(data -> {
                var postgresServerConnection = connectionReference.get();

                if (postgresServerConnection == null) {
                    postgresServerConnection = vertx.createNetClient(new NetClientOptions())
                            .connect(5432, "localhost")
                            .onSuccess(netClientSocket -> {
                                log.info("Connected to: {}", netClientSocket.remoteAddress());

                                log.info("Postgres Client said: {}", data);
                                netClientSocket.write(data);

                                netClientSocket.handler(data1 -> {
                                    log.info("Postgres Server said: {}", data1);
                                    socket.write(data1);
                                });

                                netClientSocket.closeHandler(v -> {
                                    log.info("The socket has been closed");
                                    socket.close();
                                });
                            })
                            .onFailure(error -> log.error("Failed to connect to client postgres"))
                            .onFailure(error -> socket.end());
                    connectionReference.set(postgresServerConnection);
                } else {


                    var actualSocket = postgresServerConnection
                            .result();
                    log.info("Postgres Client said: {}", data);
                    actualSocket.write(data);

                    actualSocket.handler(data1 -> {
                        log.info("Postgres Server said: {}", data1);
                        socket.write(data1);
                    });

                }
            });
            socket.closeHandler(v -> log.info("Client socket has been closed"));
        });

        netServer
                .listen(5433, "localhost")
                .onSuccess(server -> log.info("TCP Server Started ... {}", server.actualPort()))
                .onSuccess(server -> startPromise.complete())
                .onFailure(startPromise::fail);

    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        netServer.close(result -> stopPromise.complete());
    }
}
