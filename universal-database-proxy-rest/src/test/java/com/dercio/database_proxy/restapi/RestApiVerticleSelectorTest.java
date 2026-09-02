package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.ApiConfig;
import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.router.RouterFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestApiVerticleSelectorTest {

    @Mock
    private RouterFactory routerFactory;
    @Mock
    private RestApiHandler restApiHandler;
    @Mock
    private Repository repository;
    @Mock
    private ApiConfig apiConfig;

    private final RestApiVerticleSelector selector = new RestApiVerticleSelector();

    @Test
    void shouldSelectDelayedStartupVerticleWhenStartupDelayIsPositive() {
        when(apiConfig.getStartupDelay()).thenReturn(5L);

        var verticle = selector.select(routerFactory, restApiHandler, repository, apiConfig);

        assertInstanceOf(DelayedStartupRestApiVerticle.class, verticle);
    }

    @Test
    void shouldPreferDelayedStartupOverReloadWhenBothArePositive() {
        when(apiConfig.getStartupDelay()).thenReturn(5L);
        lenient().when(apiConfig.getReloadFrequency()).thenReturn(10L);

        var verticle = selector.select(routerFactory, restApiHandler, repository, apiConfig);

        assertInstanceOf(DelayedStartupRestApiVerticle.class, verticle);
    }

    @Test
    void shouldSelectDynamicVerticleWhenOnlyReloadFrequencyIsPositive() {
        when(apiConfig.getStartupDelay()).thenReturn(0L);
        when(apiConfig.getReloadFrequency()).thenReturn(10L);

        var verticle = selector.select(routerFactory, restApiHandler, repository, apiConfig);

        assertInstanceOf(DynamicRestApiVerticle.class, verticle);
    }

    @Test
    void shouldSelectFixedVerticleWhenNeitherDelayNorReloadIsConfigured() {
        when(apiConfig.getStartupDelay()).thenReturn(0L);
        when(apiConfig.getReloadFrequency()).thenReturn(0L);

        var verticle = selector.select(routerFactory, restApiHandler, repository, apiConfig);

        assertInstanceOf(FixedRestApiVerticle.class, verticle);
    }
}
