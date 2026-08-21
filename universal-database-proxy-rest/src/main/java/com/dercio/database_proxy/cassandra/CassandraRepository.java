package com.dercio.database_proxy.cassandra;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.database.TableMetadata;
import com.dercio.database_proxy.common.database.TableRequest;
import com.google.inject.Inject;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CassandraRepository implements Repository {

    private final CassandraObjectDeleter deleter;
    private final CassandraObjectInserter inserter;
    private final CassandraObjectFinder finder;
    private final CassandraTableFinder tableFinder;

    @Override
    public Future<List<TableMetadata>> getTables(String database) {
        return tableFinder.findTables(database)
                .map(tables -> tables.stream().map(CassandraTableMetadata::getTableMetadata).toList());
    }

    @Override
    public Future<TableMetadata> getTableInfo(TableRequest tableOption) {
        return tableFinder.findTable(tableOption).map(CassandraTableMetadata::getTableMetadata);
    }

    @Override
    public Future<List<JsonObject>> getData(TableRequest tableOption) {
        log.info("Retrieving rows for {} | {} ", tableOption.getDatabase(), tableOption.getTable());

        return tableFinder.findTable(tableOption)
                .compose(tableMetadata -> finder.find(tableMetadata, tableOption.getQueryParams()));
    }

    @Override
    public Future<Optional<JsonObject>> getDataById(TableRequest tableOption) {
        log.info("Retrieving row for {} | {}", tableOption.getDatabase(), tableOption.getTable());

        return tableFinder.findTable(tableOption)
                .compose(tableMetadata -> finder.find(tableMetadata, tableOption.getPathParams()))
                .map(elements -> elements.stream().findFirst());
    }

    @Override
    public Future<Object> createData(TableRequest tableOption) {
        log.info("Inserting data into {} | {} ", tableOption.getDatabase(), tableOption.getTable());

        return tableFinder.findTable(tableOption)
                .compose(tableMetadata -> inserter.create(tableMetadata, tableOption.getBody()));
    }

    @Override
    public Future<Integer> updateData(TableRequest tableOption) {
        log.info("Updating data in {} | {} ", tableOption.getDatabase(), tableOption.getTable());

        return tableFinder.findTable(tableOption)
                .compose(tableMetadata ->
                        inserter.update(tableMetadata, tableOption.getBody(), tableOption.getPathParams()));
    }

    @Override
    public Future<Integer> deleteDataById(TableRequest tableOption) {
        log.info("Deleting item from {} | {} ", tableOption.getDatabase(), tableOption.getTable());

        return tableFinder.findTable(tableOption)
                .compose(tableMetadata -> deleter.deleteDataById(tableMetadata, tableOption.getPathParams()));
    }

    @Override
    public Future<Integer> deleteData(TableRequest tableOption) {
        return tableFinder.findTable(tableOption)
                .compose(tableMetadata -> deleter.deleteData(tableMetadata, tableOption.getQueryParams()));
    }
}
