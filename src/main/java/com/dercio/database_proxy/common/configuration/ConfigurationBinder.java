package com.dercio.database_proxy.common.configuration;

import com.dercio.database_proxy.common.AnnotationProcessor;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.reflections.Reflections;

import java.util.Optional;
import java.util.function.Function;

import static org.reflections.scanners.Scanners.TypesAnnotated;

@Log4j2
@RequiredArgsConstructor
public class ConfigurationBinder implements AnnotationProcessor<JsonObject> {

    private final Function<TypeLiteral<Object>, AnnotatedBindingBuilder<Object>> bindingBuilder;

    @Override
    public void process(JsonObject config) {
        new Reflections(basePackage())
                .get(TypesAnnotated.with(Configuration.class).asClass())
                .forEach(aClass -> {
                    var configName = aClass.getAnnotation(Configuration.class).name();
                    Optional.ofNullable(config.getJsonObject(configName))
                            .map(rawObject -> rawObject.mapTo(aClass))
                            .ifPresentOrElse(
                                    instance -> bindClassToInstance(aClass, instance),
                                    () -> warn(aClass, configName)
                            );
                });


    }

    private void bindClassToInstance(Class<?> aClass, Object instance) {
        bindingBuilder
                .apply((TypeLiteral<Object>) TypeLiteral.get(aClass))
                .toInstance(instance);
    }

    private void warn(Class<?> aClass, String configName) {
        log.warn("Config for [{}] named [{}] was not found", aClass, configName);
    }
}
