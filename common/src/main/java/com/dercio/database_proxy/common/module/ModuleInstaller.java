package com.dercio.database_proxy.common.module;

import com.dercio.database_proxy.common.AnnotationProcessor;
import com.google.inject.Module;
import lombok.SneakyThrows;
import org.reflections.Reflections;

import java.util.function.Consumer;

public class ModuleInstaller implements AnnotationProcessor<Consumer<Module>> {

    @Override
    public void process(Consumer<Module> install) {
        new Reflections(basePackage())
                .getTypesAnnotatedWith(GuiceModule.class)
                .stream()
                .map(this::createInstance)
                .filter(Module.class::isInstance)
                .map(Module.class::cast)
                .forEach(install);
    }

    @SneakyThrows
    private Object createInstance(Class<?> aClass) {
        return aClass.getDeclaredConstructor().newInstance();
    }
}
