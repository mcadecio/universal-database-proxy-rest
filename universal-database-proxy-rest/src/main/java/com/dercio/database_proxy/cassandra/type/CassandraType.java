package com.dercio.database_proxy.cassandra.type;

import com.dercio.database_proxy.openapi.OpenApiType;
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
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Maps CQL type names (as spelled in {@code system_schema.columns.type}) to OpenAPI types and to the
 * Java types the DataStax driver expects.
 *
 * <p>Unlike JDBC, the DataStax driver binds values by <b>exact</b> Java type and rejects mismatches —
 * passing a {@code String} for a {@code uuid} column or an {@code Integer} for a {@code bigint} column
 * is an error, not a silent coercion. Every value therefore has to be converted here before it is
 * bound.
 *
 * <p>Collection types ({@code list<text>}, {@code set<int>}, {@code map<text, text>}) and UDTs do not
 * match any constant and fall through to {@link #UNKNOWN}, which surfaces them as OpenAPI
 * {@code ANY} and passes them through untouched.
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

    /**
     * Converts a JSON body value into the Java type the driver expects. Going via the string form
     * keeps this uniform: every supported CQL type can be reconstructed from its text
     * representation, and JSON only ever hands us strings, numbers and booleans.
     */
    public Object toSqlValue(Object value) {
        if (value == null) return null;

        return parse(String.valueOf(value));
    }

    /**
     * Converts a value read back from the driver into something {@code JsonObject} can encode.
     * {@code UUID}, {@code LocalDate}, {@code LocalTime}, {@code InetAddress} and {@code ByteBuffer}
     * have no JSON representation, so they are rendered as strings.
     */
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
        return from(type).toSqlValue(value);
    }

    public static Object toRowValue(String type, Object value) {
        return from(type).toRowValue(value);
    }

    public static String toOpenApiType(String type) {
        return from(type).getOpenApiType();
    }

    public static Object parse(String type, String value) {
        return from(type).parse(value);
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
