package com.dercio.database_proxy.server;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class HttpConfig {

    public Router configureRouter(Router router) {
        router.route().handler(BodyHandler.create());
        router.route().handler(createDefaultCorsHandler());
        return router;
    }

    public Handler<RoutingContext> createDefaultCorsHandler() {
        return CorsHandler.create()
                .addOrigins(getAllowedDomains())
                .allowedMethod(io.vertx.core.http.HttpMethod.GET)
                .allowedMethod(io.vertx.core.http.HttpMethod.POST)
                .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
                .allowedHeader("Access-Control-Request-Method")
                .allowedHeader("Access-Control-Allow-Origin")
                .allowedHeader("Access-Control-Allow-Headers")
                .allowedHeader("Content-Type");
    }

    public List<String> getAllowedDomains() {
        return Stream.of(System.getProperty("cors.allowed.domain", "http://localhost:3000"))
                .map(string -> string.split(","))
                .flatMap(Stream::of)
                .collect(Collectors.toList());
    }

    public int getPort() {
        return Integer.parseInt(System.getProperty("server.port", "1234"));
    }

}
