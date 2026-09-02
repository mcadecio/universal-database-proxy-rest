package com.dercio.database_proxy.cassandra.type;

import com.dercio.database_proxy.common.database.OpenApiColumnType;
import com.dercio.database_proxy.openapi.OpenApiType;
import io.vertx.core.json.JsonArray;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps CQL type names (as spelled in {@code system_schema.columns.type}) to OpenAPI types and to the
 * Java types the DataStax driver expects. Unlike JDBC, the driver binds by <b>exact</b> Java type and
 * rejects mismatches, so every value has to be converted here before it is bound.
 *
 * <p>{@code set<T>} is handled outside the enum because a parameterized type cannot be a constant.
 * Other collections and UDTs fall through to {@link #UNKNOWN} and are passed through untouched.
 */
@RequiredArgsConstructor
public enum CassandraType {
    ASCII("ascii", OpenApiType.STRING, Function.identity()),
    TEXT("text", OpenApiType.STRING, Function.identity()),
    VARCHAR("varchar", OpenApiType.STRING, Function.identity()),
    INT("int", OpenApiType.INTEGER, Integer::parseInt),
    BIGINT("bigint", OpenApiType.INTEGER, Long::parseLong),
    SMALLINT("smallint", OpenApiType.INTEGER, Short::parseShort),
    TINYINT("tinyint", OpenApiType.INTEGER, Byte::parseByte),
    VARINT("varint", OpenApiType.INTEGER, BigInteger::new),
    COUNTER("counter", OpenApiType.INTEGER, Long::parseLong),
    FLOAT("float", OpenApiType.NUMBER, Float::parseFloat),
    DOUBLE("double", OpenApiType.NUMBER, Double::parseDouble),
    DECIMAL("decimal", OpenApiType.NUMBER, BigDecimal::new),
    BOOLEAN("boolean", OpenApiType.BOOLEAN, Boolean::parseBoolean),
    UUID("uuid", OpenApiType.STRING, java.util.UUID::fromString),
    TIMEUUID("timeuuid", OpenApiType.STRING, java.util.UUID::fromString),
    TIMESTAMP("timestamp", OpenApiType.STRING, Instant::parse),
    DATE("date", OpenApiType.STRING, LocalDate::parse),
    TIME("time", OpenApiType.STRING, LocalTime::parse),
    INET("inet", OpenApiType.STRING, CassandraType::toInetAddress),
    BLOB("blob", OpenApiType.STRING, CassandraType::toByteBuffer),
    UNKNOWN("UNKNOWN", OpenApiType.ANY, Function.identity());

    @Getter
    private final String dbType;

    @Getter
    private final String openApiType;

    private final Function<String, ?> mapper;

    @SuppressWarnings("unchecked")
    public <T> T parse(String value) {
        var sanitizedValue = blankToNull(value);

        return sanitizedValue == null ? null : (T) mapper.apply(sanitizedValue);
    }

    /** Going via the string form is uniform: every supported CQL type parses back from its text. */
    public Object toSqlValue(Object value) {
        if (value == null) return null;

        return parse(String.valueOf(value));
    }

    /** Driver types with no JSON representation are rendered as strings. */
    public Object toRowValue(Object value) {
        if (value == null) return null;

        return switch (this) {
            case UUID, TIMEUUID, TIMESTAMP, DATE, TIME -> value.toString();
            case INET -> value instanceof InetAddress address ? address.getHostAddress() : value.toString();
            case BLOB -> value instanceof ByteBuffer buffer ? encodeBase64(buffer) : value.toString();
            default -> value;
        };
    }

    public static Object toSqlValue(String type, Object value) {
        var elementType = setElementType(type);

        return elementType == null ? from(type).toSqlValue(value) : toSqlSet(elementType, value);
    }

    public static Object toRowValue(String type, Object value) {
        var elementType = setElementType(type);

        return elementType == null ? from(type).toRowValue(value) : toRowArray(elementType, value);
    }

    public static String toOpenApiType(String type) {
        return isSet(type) ? OpenApiType.ARRAY : from(type).getOpenApiType();
    }

    /** The resolver {@code ColumnMetadata} needs: the OpenAPI type plus, for sets, its element type. */
    public static OpenApiColumnType toOpenApiColumnType(String type) {
        return new OpenApiColumnType(toOpenApiType(type), toOpenApiItemsType(type));
    }

    /** The element type of an array column, or {@code null} for a scalar one. */
    public static String toOpenApiItemsType(String type) {
        var elementType = setElementType(type);

        return elementType == null ? null : from(elementType).getOpenApiType();
    }

    /** For a set column this yields a single element, not a set, because sets filter with CONTAINS. */
    public static Object parse(String type, String value) {
        var elementType = setElementType(type);

        return from(elementType == null ? type : elementType).parse(value);
    }

    public static boolean isSet(String type) {
        return setElementType(type) != null;
    }

    /** The element type of {@code set<T>} or {@code frozen<set<T>>}, or {@code null} otherwise. */
    public static String setElementType(String type) {
        if (type == null) {
            return null;
        }

        var normalized = unwrap(type.trim(), "frozen<");

        return unwrapOrNull(normalized, "set<");
    }

    private static String unwrap(String type, String prefix) {
        var unwrapped = unwrapOrNull(type, prefix);

        return unwrapped == null ? type : unwrapped;
    }

    private static String unwrapOrNull(String type, String prefix) {
        if (type.length() <= prefix.length()
                || !type.regionMatches(true, 0, prefix, 0, prefix.length())
                || !type.endsWith(">")) {
            return null;
        }

        return type.substring(prefix.length(), type.length() - 1).trim();
    }

    private static Object toSqlSet(String elementType, Object value) {
        if (value == null) {
            return null;
        }

        return elementsOf(value)
                .stream()
                .map(element -> toSqlValue(elementType, element))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Object toRowArray(String elementType, Object value) {
        if (value == null) {
            return null;
        }

        var array = new JsonArray();
        elementsOf(value).forEach(element -> array.add(toRowValue(elementType, element)));

        return array;
    }

    private static List<Object> elementsOf(Object value) {
        if (value instanceof JsonArray jsonArray) {
            return jsonArray.getList();
        }

        if (value instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }

        return List.of(value);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    @SneakyThrows
    private static InetAddress toInetAddress(String value) {
        return InetAddress.getByName(value);
    }

    private static ByteBuffer toByteBuffer(String value) {
        try {
            return ByteBuffer.wrap(Base64.getDecoder().decode(value));
        } catch (IllegalArgumentException e) {
            return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String encodeBase64(ByteBuffer buffer) {
        var readOnlyCopy = buffer.asReadOnlyBuffer();
        var bytes = new byte[readOnlyCopy.remaining()];
        readOnlyCopy.get(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static CassandraType from(String type) {
        if (type == null) {
            return UNKNOWN;
        }

        var desiredType = type.toUpperCase();

        return Stream.of(values())
                .filter(cassandraType -> cassandraType.toString().equals(desiredType))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
