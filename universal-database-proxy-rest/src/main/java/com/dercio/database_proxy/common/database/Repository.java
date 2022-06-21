package com.dercio.database_proxy.common.database;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Repository {
    Future<Table> getTableInfo(TableRequest tableRequest);

    Future<List<JsonObject>> getData(TableRequest tableOption);

    Future<Optional<JsonObject>> getDataById(
           TableRequest tableOption,
            Map<String, String> pathParams
    );

    Future<Object> createData(TableRequest tableOption, JsonObject data);

    Future<Void> updateData(
            TableRequest tableOption,
            JsonObject data,
            Map<String, String> pathParams
    );

    Future<Integer> deleteData(TableRequest tableOption, Map<String, String> pathParams);
}
