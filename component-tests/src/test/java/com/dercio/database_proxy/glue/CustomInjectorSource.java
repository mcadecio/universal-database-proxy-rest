package com.dercio.database_proxy.glue;

import com.dercio.database_proxy.common.mapper.MapperModule;
import com.dercio.database_proxy.repositories.budgets.BudgetsRepository;
import com.dercio.database_proxy.repositories.cars.CarsRepository;
import com.dercio.database_proxy.repositories.football.NationalFootballTeamsRepository;
import com.dercio.database_proxy.repositories.wheel.WheelsRepository;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.PrivateModule;
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
                new PrivateModule() {
                    @Override
                    protected void configure() {
                        install(new XMLMyBatisModule() {
                            @SneakyThrows
                            @Override
                            protected void initialize() {
                                install(JdbcHelper.PostgreSQL);
                                setEnvironmentId("test");
                                setClassPathResource("batis/postgres/mybatis-config.xml");
                                addProperties(Resources.getResourceAsProperties("batis/postgres/postgres.properties"));
                            }
                        });

                        expose(BudgetsRepository.class);
                        expose(NationalFootballTeamsRepository.class);
                    }
                },
                new PrivateModule() {
                    @Override
                    protected void configure() {
                        install(new XMLMyBatisModule() {
                            @SneakyThrows
                            @Override
                            protected void initialize() {
                                install(JdbcHelper.PostgreSQL);
                                setEnvironmentId("test");
                                setClassPathResource("batis/cockroach/mybatis-config.xml");
                                addProperties(Resources.getResourceAsProperties("batis/cockroach/cockroach.properties"));
                            }
                        });

                        expose(CarsRepository.class);
                        expose(WheelsRepository.class);
                    }
                },
                CucumberModules.createScenarioModule(),
                new MapperModule()
        );
    }
}
