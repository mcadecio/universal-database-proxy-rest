package com.dercio.database_proxy.common;

import com.dercio.database_proxy.common.module.GuiceModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import java.util.Map;

@GuiceModule
public class SystemModule extends AbstractModule {

    @Provides
    @Named("system.env.variables")
    public Map<String, String> providesEnvVariables() {
        return System.getenv();
    }
}
