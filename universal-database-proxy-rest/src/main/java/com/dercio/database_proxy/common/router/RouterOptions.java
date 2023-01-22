package com.dercio.database_proxy.common.router;

import com.dercio.database_proxy.restapi.RestApiHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder(setterPrefix = "with")
public class RouterOptions {
    private final RestApiHandler restApiHandler;
    private String openApiFilePath;
    private Handler<RoutingContext> failureHandler;
    private Handler<RoutingContext> notFoundHandler;


    public static class RouterOptionsBuilder {
        private RouterOptionsBuilder() {
        }
    }

    public static RouterOptionsBuilder builder(RestApiHandler restApiHandler, String openApiFilePath) {
        Objects.requireNonNull(restApiHandler, "Rest Api Handler cannot be null");
        Objects.requireNonNull(openApiFilePath, "Open Api File Path cannot be null");
        return new RouterOptionsBuilder()
                .withRestApiHandler(restApiHandler)
                .withOpenApiFilePath(openApiFilePath);
    }
}
