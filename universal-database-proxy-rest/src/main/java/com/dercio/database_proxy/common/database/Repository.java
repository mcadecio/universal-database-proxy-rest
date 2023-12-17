package com.dercio.database_proxy.common.database;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;

public interface Repository {
    Future<List<TableMetadata>> getTables(String database);

    Future<TableMetadata> getTableInfo(TableRequest tableRequest);

    Future<List<JsonObject>> getData(TableRequest tableRequest);

    Future<Optional<JsonObject>> getDataById(TableRequest tableOption);

    Future<Object> createData(TableRequest tableOption);

    Future<Integer> updateData(TableRequest tableOption);

    Future<Integer> deleteDataById(TableRequest tableOption);

    Future<Integer> deleteData(TableRequest tableOption);
}
