package com.dercio.database_proxy.proxy;

import com.google.inject.Inject;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProxyHandler implements Handler<NetSocket> {

    private static final String VERTX_NET = "__vertx.net.";
    private final Map<String, NetSocket> clientIdServerSocket = new ConcurrentHashMap<>();
    private final Vertx vertx;
    private final ProxyConfig proxyConfig;

    @Override
    public void handle(NetSocket clientSocket) {
        var destinationConfig = proxyConfig.getDestination();
        var clientId = clientSocket.writeHandlerID();
        logInfo(clientId, "Client connected to Proxy. Client: {}", clientSocket.remoteAddress());
        clientSocket.handler(data -> {

            if (!clientIdServerSocket.containsKey(clientId)) {
                vertx.createNetClient()
                        .connect(destinationConfig.getPort(), destinationConfig.getHost())
                        .onSuccess(serverSocket -> clientIdServerSocket.put(clientId, serverSocket))
                        .onSuccess(serverSocket -> mountServerSocketHandlers(clientSocket, clientId, data, serverSocket))
                        .onFailure(error -> logError(clientId, "Proxy failed to connect to Postgres Server"))
                        .onFailure(error -> clientSocket.end());
            } else {
                logDebug(clientId, "Postgres Client said: {}", data);
                clientIdServerSocket.get(clientId).write(data);
            }
        });

        clientSocket.closeHandler(v -> {
            logInfo(clientId, "Client disconnected from Proxy");

            Optional.ofNullable(clientIdServerSocket.get(clientId))
                    .ifPresent(NetSocket::end);

            clientIdServerSocket.remove(clientId);
        });
    }

    private void mountServerSocketHandlers(
            NetSocket clientSocket,
            String clientId,
            Buffer clientData,
            NetSocket serverSocket
    ) {
        logInfo(clientId, "Proxy connected to Postgres Server: Proxy Server: {}", serverSocket.remoteAddress());
        logInfo(clientId, "Passthrough connection open");

        serverSocket.handler(serverData -> {
            logDebug(clientId, "Postgres Server said: {}", serverData);
            clientSocket.write(serverData);
        });

        serverSocket.closeHandler(unused -> {
            logInfo(clientId, "Proxy disconnected from Postgres Server");
            clientSocket.end();
        });

        logDebug(clientId, "Postgres Client said: {}", clientData);
        serverSocket.write(clientData);
    }


    private void logInfo(String socketId, String message, Object... params) {
        log.info(
                "{}|{}{}",
                socketId.replace(VERTX_NET, ""),
                params,
                message
        );
    }

    private void logDebug(String socketId, String message, Object... params) {
        log.debug(
                "{}|" + message,
                socketId.replace(VERTX_NET, ""),
                params
        );
    }

    private void logError(String socketId, String message, Object... params) {
        log.error(
                "{}|" + message,
                socketId.replace(VERTX_NET, ""),
                params
        );
    }
}
