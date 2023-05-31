package com.dercio.database_proxy.cars.type.handlers;

import com.dercio.database_proxy.cars.ExtraDetails;
import com.dercio.database_proxy.common.mapper.Mapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.*;
import java.util.Optional;

@MappedJdbcTypes(JdbcType.JAVA_OBJECT)
public class ExtraDetailsTypeHandler extends BaseTypeHandler<ExtraDetails> {

    private final Mapper mapper = new Mapper(new ObjectMapper());

    @Override
    @SneakyThrows
    public void setNonNullParameter(PreparedStatement ps, int i, ExtraDetails parameter, JdbcType jdbcType) {
        ps.setString(i, mapper.encode(parameter));
    }

    @Override
    public ExtraDetails getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return Optional.ofNullable(rs.getString(columnName))
                .map(rawJson -> mapper.decode(rawJson, ExtraDetails.class))
                .orElse(null);
    }

    @Override
    public ExtraDetails getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return Optional.ofNullable(rs.getString(columnIndex))
                .map(rawJson -> mapper.decode(rawJson, ExtraDetails.class))
                .orElse(null);
    }

    @Override
    public ExtraDetails getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return Optional.ofNullable(cs.getString(columnIndex))
                .map(rawJson -> mapper.decode(rawJson, ExtraDetails.class))
                .orElse(null);
    }
}
