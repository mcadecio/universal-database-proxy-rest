package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.exceptions.VerticleDisabledException;
import com.dercio.database_proxy.common.handlers.FailureHandler;
import com.dercio.database_proxy.common.handlers.NotFoundHandler;
import com.dercio.database_proxy.openapi.OpenApiCreator;
import com.google.inject.Inject;
import io.swagger.v3.core.util.Yaml;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RestApiVerticle extends AbstractVerticle {

    private final FailureHandler failureHandler;
    private final NotFoundHandler notFoundHandler;
    private final RestApiHandler restApiHandler;
    private final Repository pgRepository;
    private final ApiConfig apiConfig;
    private HttpServer httpServer;

    @Override
    public void start(Promise<Void> startPromise) {

        if (!apiConfig.isEnabled()) {
            startPromise.fail(new VerticleDisabledException(getClass().getSimpleName()));
            return;
        }
        vertx.setTimer(apiConfig.getStartupDelay(), timerId ->
                pgRepository.getTables(apiConfig.getDatabase().getDatabaseName())
                        .compose(this::createOpenApiFile)
                        .compose(this::createRouterAndMountHandlers)
                        .onSuccess(router -> {
                            var httpServerOptions = new HttpServerOptions()
                                    .setUseAlpn(true)
                                    .setSsl(false);
                            httpServer = vertx.createHttpServer(httpServerOptions);
                            httpServer
                                    .requestHandler(router)
                                    .listen(apiConfig.getPort(), apiConfig.getHost())
                                    .onSuccess(event -> log.info("Rest API Server Started ... {}", event.actualPort()))
                                    .onSuccess(event -> startPromise.complete())
                                    .onFailure(startPromise::fail);
                        })
                        .onFailure(startPromise::fail));

    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        httpServer.close()
                .onSuccess(event -> log.info("Goodbye Rest API server ..."))
                .onFailure(error -> log.error(error.getMessage()))
                .onComplete(event -> stopPromise.complete());
    }

    private Future<List<TableMetadata>> createOpenApiFile(List<TableMetadata> tables) {
        var openAPI = OpenApiCreator.create(tables);
        return vertx.fileSystem()
                .writeFile(apiConfig.getOpenApiFilePath(), Buffer.buffer(Yaml.pretty(openAPI)))
                .map(tables)
                .onSuccess(unused -> log.info("OpenApi File created"))
                .onFailure(error -> log.error("Failed to create OpenApi File: {}", error.getMessage()));
    }

    private Future<Router> createRouterAndMountHandlers(List<TableMetadata> tables) {
        return RouterBuilder.create(vertx, apiConfig.getOpenApiFilePath())
                .map(routerBuilder -> {
                    Map<String, Handler<RoutingContext>> mappings = new HashMap<>();

                    mappings.put("getOpenApi", rc -> rc.response()
                            .setChunked(true)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end(routerBuilder.getOpenAPI().getOpenAPI().encode()));

                    tables.forEach(table -> {
                        mappings.put("get_" + table.getTableName(), restApiHandler::getResources);
                        mappings.put("create_" + table.getTableName(), restApiHandler::createResource);
                        var byId = "_by_id";
                        mappings.put("get_" + table.getTableName() + byId, restApiHandler::getResourceById);
                        mappings.put("update_" + table.getTableName() + byId, restApiHandler::updateResourceById);
                        mappings.put("delete_" + table.getTableName() + byId, restApiHandler::deleteResourceById);
                    });

                    mappings.forEach((operationId, handler) -> {
                        var operation = routerBuilder.operation(operationId);
                        operation.handler(rc -> {
                            operation.getOperationModel()
                                    .getJsonObject("x-metadata")
                                    .getMap()
                                    .forEach(rc::put);
                            handler.handle(rc);
                        });
                    });

                    return routerBuilder.createRouter();
                })
                .onSuccess(router -> {
                    router.route("/docs/*").handler(StaticHandler.create());
                    router.route().failureHandler(failureHandler);
                    router.route().last().handler(notFoundHandler);
                });
    }
}