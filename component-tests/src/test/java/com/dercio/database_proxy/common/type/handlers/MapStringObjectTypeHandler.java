package com.dercio.database_proxy.common.type.handlers;

import com.dercio.database_proxy.common.mapper.Mapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@MappedJdbcTypes(JdbcType.JAVA_OBJECT)
public class MapStringObjectTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    private final Mapper mapper = new Mapper(new ObjectMapper());

    @Override
    @SneakyThrows
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) {
        ps.setString(i, mapper.encode(parameter));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return Optional.ofNullable(rs.getString(columnName))
                .map(rawJson -> mapper.decode(rawJson, new TypeReference<Map<String, Object>>() {
                }))
                .orElse(null);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return Optional.ofNullable(rs.getString(columnIndex))
                .map(rawJson -> mapper.decode(rawJson, new TypeReference<Map<String, Object>>() {
                }))
                .orElse(null);
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return Optional.ofNullable(cs.getString(columnIndex))
                .map(rawJson -> mapper.decode(rawJson, new TypeReference<Map<String, Object>>() {
                }))
                .orElse(null);
    }
}
