package com.dercio.database_proxy.postgres;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PgTypeMapper {
    public static final String STRING = "string";
    public static final String INTEGER = "integer";
    private static final String NUMERIC = "numeric";
    private static final String BIGINT = "bigint";
    private static final String DATE = "date";
    private static final String CHARACTER_VARYING = "character varying";
    private static final String BOOLEAN = "boolean";
    private static final String NUMBER = "number";
    private static final String OBJECT = "object";
    private static final Map<String, String> PG_TYPE_TO_SWAGGER_TYPE = ImmutableMap.<String, String>builder()
            .put(INTEGER, INTEGER)
            .put(NUMERIC, NUMBER)
            .put(BIGINT, INTEGER)
            .put(BOOLEAN, BOOLEAN)
            .put(DATE, STRING)
            .put(CHARACTER_VARYING, STRING)
            .put("uuid", STRING)
            .put("text", STRING)
            .put("character", STRING)
            .put("timestamp without time zone", STRING)
            .put("timestamp with time zone", STRING)
            .put("json", OBJECT)
            .put("jsonb", OBJECT)
            .build();

    public static String fromPgToSwagger(String pgType) {
        return PG_TYPE_TO_SWAGGER_TYPE.get(pgType);
    }
}
