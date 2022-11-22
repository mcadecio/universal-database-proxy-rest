package com.dercio.database_proxy.postgres;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.database.TableRequest;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PgRepository implements Repository {

    private final PgObjectDeleter deleter;
    private final PgObjectInserter inserter;
    private final PgObjectFinder finder;
    private final PgTableFinder tableFinder;

    @Override
    public Future<List<TableMetadata>> getTables(String database) {
        return tableFinder.findTables(database);
    }

    @Override
    public Future<TableMetadata> getTableInfo(TableRequest tableOption) {
       return tableFinder.findTable(tableOption);
    }

    @Override
    public Future<List<JsonObject>> getData(TableRequest tableOption) {
        log.info("Retrieving rows for {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption).compose(finder::find);
    }

    @Override
    public Future<Optional<JsonObject>> getDataById(TableRequest tableOption, Map<String, String> pathParams) {
        log.info("Retrieving row for {} | {} | {}",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableMetadata -> finder.findById(tableMetadata, pathParams));
    }

    @Override
    public Future<Object> createData(TableRequest tableOption, JsonObject data) {
        log.info("Inserting data into {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableMetadata -> inserter.create(tableMetadata, data));
    }

    @Override
    public Future<Integer> updateData(
            TableRequest tableOption,
            JsonObject data,
            Map<String, String> pathParams) {

        log.info("Updating data in {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableMetadata -> inserter.update(tableMetadata, data, pathParams));
    }

    @Override
    public Future<Integer> deleteData(TableRequest tableOption, Map<String, String> pathParams) {
        log.info("Deleting item from {} | {} | {} ",
                tableOption.getDatabase(),
                tableOption.getSchema(),
                tableOption.getTable()
        );

        return getTableInfo(tableOption)
                .compose(tableMetadata -> deleter.deleteData(tableMetadata, pathParams));
    }

}
