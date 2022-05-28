package com.dercio.database_proxy.common.module;

import com.dercio.database_proxy.common.AnnotationProcessor;
import com.google.inject.AbstractModule;
import lombok.SneakyThrows;
import org.reflections.Reflections;

import java.util.function.Consumer;

import static org.reflections.scanners.Scanners.TypesAnnotated;

public class ModuleInstaller implements AnnotationProcessor<Consumer<AbstractModule>> {

    @Override
    public void process(Consumer<AbstractModule> install) {
        new Reflections(basePackage())
                .get(TypesAnnotated.with(Module.class).asClass())
                .stream()
                .map(this::createInstance)
                .filter(AbstractModule.class::isInstance)
                .map(AbstractModule.class::cast)
                .forEach(install);
    }

    @SneakyThrows
    private Object createInstance(Class<?> aClass) {
        return aClass.getDeclaredConstructor().newInstance();
    }
}
