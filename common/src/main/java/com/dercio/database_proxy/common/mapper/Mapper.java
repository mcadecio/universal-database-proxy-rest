package com.dercio.database_proxy.common.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Mapper {

    private final ObjectMapper objectMapper;

    public String encode(Object value) {
        String stringfiedValue = null;

        try {
            stringfiedValue = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Error encoding value: {}", e.getMessage(), e);
        }

        return stringfiedValue;
    }

    public <T> T decode(String value, TypeReference<T> valueTypeRef) {
        T mappedValue = null;

        try {
            mappedValue = objectMapper.readValue(value, valueTypeRef);
        } catch (JsonProcessingException e) {
            log.error("Error decoding value: {}", e.getMessage(), e);
        }

        return mappedValue;
    }

}
