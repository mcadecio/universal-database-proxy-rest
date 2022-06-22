package com.dercio.database_proxy.glue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.cucumber.guice.CucumberModules;
import io.cucumber.guice.InjectorSource;
import lombok.SneakyThrows;
import org.apache.ibatis.io.Resources;
import org.mybatis.guice.XMLMyBatisModule;
import org.mybatis.guice.datasource.helper.JdbcHelper;

public class CustomInjectorSource implements InjectorSource {
    @Override
    public Injector getInjector() {
        return Guice.createInjector(
                new XMLMyBatisModule() {
                    @SneakyThrows
                    @Override
                    protected void initialize() {
                        install(JdbcHelper.PostgreSQL);
                        setEnvironmentId("test");
                        setClassPathResource("batis/mybatis-config.xml");
                        addProperties(Resources.getResourceAsProperties("batis/postgres.properties"));
                    }
                },
                CucumberModules.createScenarioModule()
        );
    }
}
