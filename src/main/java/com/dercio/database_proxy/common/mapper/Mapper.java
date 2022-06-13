package com.dercio.database_proxy.common.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Mapper {

    private final ObjectMapper mapper;

    public String encode(Object value) {
        String stringfiedValue = null;

        try {
            stringfiedValue = mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Error encoding value: {}", e.getMessage(), e);
        }

        return stringfiedValue;
    }

}
