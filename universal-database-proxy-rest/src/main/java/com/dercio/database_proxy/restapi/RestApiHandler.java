package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.database.TableRequest;
import com.dercio.database_proxy.common.error.ErrorResponse;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.stream.Collectors;

import static com.simplaex.http.StatusCode._201;
import static com.simplaex.http.StatusCode._204;
import static com.simplaex.http.StatusCode._404;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RestApiHandler {

    private static final String DATABASE = "database";
    private static final String SCHEMA = "schema";
    private static final String TABLE = "table";

    private final Mapper mapper;
    private final Repository repository;

    public void getResources(RoutingContext event) {
        var tableOption = createTableOption(event);

        repository.getData(tableOption)
                .map(items -> items.stream()
                        .map(JsonObject::encode)
                        .toList()
                        .toString())
                .onSuccess(rows -> event.response()
                        .setChunked(true)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(rows))
                .onFailure(event::fail);
    }


    public void getResourceById(RoutingContext event) {
        var tableOption = createTableOption(event);

        repository.getDataById(tableOption)
                .onSuccess(optionalJsonObject -> {
                    var response = event.response()
                            .setChunked(true)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON);

                    if (optionalJsonObject.isPresent()) {
                        response.end(optionalJsonObject.get().encode());
                    } else {
                        var error = com.dercio.database_proxy.common.error.ErrorResponse.builder()
                                .path(event.normalizedPath())
                                .code(_404.getCode())
                                .message(_404.getLabel())
                                .build();

                        response.setStatusCode(error.getCode()).end(mapper.encode(error));
                    }
                })
                .onFailure(event::fail);
    }

    public void createResource(RoutingContext event) {
        var tableOption = createTableOption(event);

        repository.createData(tableOption)
                .map(id -> {
                    var baseUrl = event.request().absoluteURI();
                    if (!baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/";
                    }
                    return baseUrl + id;
                })
                .onSuccess(uri -> event.response()
                        .setStatusCode(_201.getCode())
                        .putHeader(HttpHeaders.LOCATION, uri)
                        .end())
                .onFailure(event::fail);
    }


    public void updateResourceById(RoutingContext event) {
        var tableOption = createTableOption(event);

        repository.updateData(tableOption)
                .onSuccess(rowsUpdated -> {
                    var response = event.response();
                    if (rowsUpdated > 0) {
                        event.response().setStatusCode(_204.getCode()).end();
                    } else {
                        var error = createNotFoundResponse(event.normalizedPath());
                        response.setStatusCode(error.getCode())
                                .setChunked(true)
                                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                                .end(mapper.encode(error));
                    }
                })
                .onFailure(event::fail);
    }

    public void deleteResourceById(RoutingContext event) {
        var tableOption = createTableOption(event);

        repository.deleteDataById(tableOption)
                .onSuccess(rowsDeleted -> {
                    var response = event.response();
                    if (rowsDeleted > 0) {
                        event.response().setStatusCode(_204.getCode()).end();
                    } else {
                        var error = createNotFoundResponse(event.normalizedPath());

                        response.setStatusCode(error.getCode())
                                .setChunked(true)
                                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                                .end(mapper.encode(error));
                    }
                })
                .onFailure(event::fail);
    }

    public void deleteResources(RoutingContext event) {
        var tableOption = createTableOption(event);

        repository.deleteData(tableOption)
                .onSuccess(rowsDeleted -> {
                    var response = event.response();
                    if (rowsDeleted > 0) {
                        event.response().setStatusCode(_204.getCode()).end();
                    } else {
                        var error = createNotFoundResponse(event.normalizedPath());

                        response.setStatusCode(error.getCode())
                                .setChunked(true)
                                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                                .end(mapper.encode(error));
                    }
                })
                .onFailure(event::fail);
    }

    private TableRequest createTableOption(RoutingContext rc) {
        Map<String, String> queryParams = rc.queryParams().entries()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new TableRequest(
                rc.get(DATABASE),
                rc.get(SCHEMA),
                rc.get(TABLE),
                queryParams,
                rc.pathParams(),
                rc.body().asJsonObject()
        );
    }

    private ErrorResponse createNotFoundResponse(String path) {
        return ErrorResponse.builder()
                .path(path)
                .code(_404.getCode())
                .message(_404.getLabel())
                .build();
    }
}
