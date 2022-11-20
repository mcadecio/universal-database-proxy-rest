package com.dercio.database_proxy.common.mapper;

import com.dercio.database_proxy.common.module.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

@Module
public class MapperModule extends AbstractModule {

    @Provides
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setDateFormat(new StdDateFormat())
                .registerModule(new JavaTimeModule());
    }
}
