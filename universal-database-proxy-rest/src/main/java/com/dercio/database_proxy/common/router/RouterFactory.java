package com.dercio.database_proxy.common.router;

import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.handlers.FailureHandler;
import com.dercio.database_proxy.common.handlers.NotFoundHandler;
import com.dercio.database_proxy.common.handlers.OpenApiHandler;
import com.dercio.database_proxy.openapi.OpenApiCreator;
import com.dercio.database_proxy.restapi.RestApiHandler;
import com.google.inject.Inject;
import io.swagger.v3.core.util.Yaml;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.Operation;
import io.vertx.ext.web.openapi.RouterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RouterFactory {

    private static final String GET_OPEN_API_OPERATION_ID = "getOpenApi";
    private static final String GET_RESOURCE_OPERATION_ID_FORMAT = "get_%s";
    private static final String CREATE_RESOURCE_OPERATION_ID_FORMAT = "create_%s";
    private static final String GET_RESOURCE_BY_ID_OPERATION_ID_FORMAT = "get_%s_by_id";
    private static final String UPDATE_RESOURCE_BY_ID_OPERATION_ID_FORMAT = "update_%s_by_id";
    private static final String DELETE_RESOURCE_BY_ID_OPERATION_ID_FORMAT = "delete_%s_by_id";
    private static final String DELETE_RESOURCE_OPERATION_ID_FORMAT = "delete_%s";

    private final Vertx vertx;
    private final FailureHandler defaultFailureHandler;
    private final NotFoundHandler defaultNotFoundHandler;
    private final OpenApiCreator openApiCreator;

    public Future<Router> createRouter(List<TableMetadata> tables, RouterOptions options) {
        return createOpenApiFile(tables, options.getOpenApiFilePath())
                .compose(empty -> createRouterBuilder(options.getOpenApiFilePath()))
                .map(this::addOpenApiHandler)
                .map(routerBuilder -> addOperationHandlersForEachTable(tables, routerBuilder, options.getRestApiHandler()))
                .map(RouterBuilder::createRouter)
                .map(this::addStaticFilesHandler)
                .map(router -> addFailureHandler(router, options))
                .map(router -> addNotFoundHandler(router, options));
    }

    private Future<Void> createOpenApiFile(List<TableMetadata> tables, String openApiFilePath) {
        var openAPI = openApiCreator.create(tables);

        return vertx.fileSystem()
                .writeFile(openApiFilePath, Buffer.buffer(Yaml.pretty(openAPI)))
                .onSuccess(unused -> log.debug("OpenApi File created"))
                .onFailure(error -> log.error("Failed to create OpenApi File: {}", error.getMessage()));
    }

    private Future<RouterBuilder> createRouterBuilder(String openApiFilePath) {
        return RouterBuilder.create(vertx, openApiFilePath);
    }

    private RouterBuilder addOpenApiHandler(RouterBuilder routerBuilder) {
        String encodedOpenApi = routerBuilder.getOpenAPI().getOpenAPI().encode();
        OpenApiHandler openApiHandler = new OpenApiHandler(encodedOpenApi);
        Operation getOpenApiOperation = routerBuilder.operation(GET_OPEN_API_OPERATION_ID);
        getOpenApiOperation.handler(openApiHandler);

        return routerBuilder;
    }

    private RouterBuilder addOperationHandlersForEachTable(
            List<TableMetadata> tables,
            RouterBuilder routerBuilder,
            RestApiHandler restApiHandler
    ) {
        tables.stream()
                .map(table -> createOperationIdHandlerMapping(table.getTableName(), restApiHandler))
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .forEach(operationIdHandlerEntry -> {
                    var operationId = operationIdHandlerEntry.getKey();
                    var operation = routerBuilder.operation(operationId);
                    var operationHandler = operationIdHandlerEntry.getValue();

                    mountHandlerToOperation(operation, operationHandler);
                });

        return routerBuilder;
    }

    private Router addFailureHandler(Router router, RouterOptions options) {
        Handler<RoutingContext> failureHandler = Optional.ofNullable(options.getFailureHandler())
                .orElse(defaultFailureHandler);

        router.route().failureHandler(failureHandler);
        return router;
    }

    private Router addNotFoundHandler(Router router, RouterOptions options) {
        Handler<RoutingContext> notFoundHandler = Optional.ofNullable(options.getNotFoundHandler())
                .orElse(defaultNotFoundHandler);

        router.route().last().handler(notFoundHandler);
        return router;
    }

    private Router addStaticFilesHandler(Router router) {
        router.route("/docs/*").handler(StaticHandler.create());
        return router;
    }

    private void mountHandlerToOperation(Operation operation, Handler<RoutingContext> operationHandler) {
        operation.handler(rc -> {
                    operation.getOperationModel()
                            .getJsonObject("x-metadata")
                            .getMap()
                            .forEach(rc::put);
                    rc.next();
                })
                .handler(operationHandler);
    }

    private Map<String, Handler<RoutingContext>> createOperationIdHandlerMapping(
            String tableName,
            RestApiHandler restApiHandler
    ) {
        return Map.of(
                format(GET_RESOURCE_OPERATION_ID_FORMAT, tableName), restApiHandler::getResources,
                format(CREATE_RESOURCE_OPERATION_ID_FORMAT, tableName), restApiHandler::createResource,
                format(GET_RESOURCE_BY_ID_OPERATION_ID_FORMAT, tableName), restApiHandler::getResourceById,
                format(UPDATE_RESOURCE_BY_ID_OPERATION_ID_FORMAT, tableName), restApiHandler::updateResourceById,
                format(DELETE_RESOURCE_BY_ID_OPERATION_ID_FORMAT, tableName), restApiHandler::deleteResourceById,
                format(DELETE_RESOURCE_OPERATION_ID_FORMAT, tableName), restApiHandler::deleteResources
        );
    }

}
