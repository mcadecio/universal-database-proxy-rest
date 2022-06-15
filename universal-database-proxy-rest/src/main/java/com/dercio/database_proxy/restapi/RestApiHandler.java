package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.database.TableRequest;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.google.inject.Inject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.stream.Collectors;

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
                        .collect(Collectors.toList())
                        .toString())
                .onSuccess(rows -> event.response()
                        .setChunked(true)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(rows))
                .onFailure(event::fail);
    }


    public void getResourceById(RoutingContext event) {
        var tableOption = createTableOption(event);
        Map<String, String> pathParams = event.pathParams();

        repository.getDataById(tableOption, pathParams)
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
        var body = event.body().asJsonObject();

        repository.createData(tableOption, body)
                .onSuccess(unused -> event.response().setStatusCode(_204.getCode()).end())
                .onFailure(event::fail);
    }

    public void updateResourceById(RoutingContext event) {
        var tableOption = createTableOption(event);
        var pathParams = event.pathParams();
        var data = event.body().asJsonObject();

        repository.updateData(tableOption, data, pathParams)
                .onSuccess(unused -> event.response().setStatusCode(_204.getCode()).end())
                .onFailure(event::fail);
    }

    public void deleteResourceById(RoutingContext event) {
        var tableOption = createTableOption(event);
        var pathParams = event.pathParams();

        repository.deleteData(tableOption, pathParams)
                .onSuccess(data -> event.response().setStatusCode(_204.getCode()).end())
                .onFailure(event::fail);
    }

    private TableRequest createTableOption(RoutingContext rc) {
        return new TableRequest(
                rc.get(DATABASE),
                rc.get(SCHEMA),
                rc.get(TABLE)
        );
    }
}
