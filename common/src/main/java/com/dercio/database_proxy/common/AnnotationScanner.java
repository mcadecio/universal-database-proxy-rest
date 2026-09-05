package com.dercio.database_proxy.common;

import io.github.classgraph.ClassGraph;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnnotationScanner {

    public static List<Class<?>> findAnnotatedClasses(String basePackage, Class<? extends Annotation> annotation) {
        try (var scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .acceptPackages(basePackage)
                .scan()) {
            return scanResult.getClassesWithAnnotation(annotation).loadClasses();
        }
    }
}
