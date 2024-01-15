package ru.otus.appcontainer.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public final class ClassLoaderUtils {

    public static List<Class<?>> findAllClasses(String packageName) throws IOException {
        String processedPackageName = packageName.replaceAll("[.]", "/");
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(processedPackageName);
             BufferedReader bf = new BufferedReader(new InputStreamReader(is))) {
            return bf.lines()
                    .filter(l -> l.endsWith(".class"))
                    .map(cls -> getClass(cls, packageName))
                    .collect(Collectors.toList());
        }
    }

    private static Class<?> getClass(String className, String packageName) {
        String classWithPackage = packageName + "." + className.substring(0, className.lastIndexOf('.'));
        try {
            return Class.forName(classWithPackage);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    String.format("Failed to load class %s", classWithPackage), e);
        }
    }

}
