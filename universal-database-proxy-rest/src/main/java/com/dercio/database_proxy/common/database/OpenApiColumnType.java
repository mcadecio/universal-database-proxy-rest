package com.dercio.database_proxy.common.database;

/**
 * The OpenAPI type of a column. {@code itemsType} is only set for {@code array} columns — OpenAPI 3
 * requires {@code items} whenever {@code type} is {@code array}, and a spec that omits it fails
 * validation when the router loads it.
 *
 * @param type      the OpenAPI type, e.g. {@code string} or {@code array}
 * @param itemsType the OpenAPI type of the elements, or {@code null} for a scalar column
 */
public record OpenApiColumnType(String type, String itemsType) {

    public static OpenApiColumnType scalar(String type) {
        return new OpenApiColumnType(type, null);
    }
}
