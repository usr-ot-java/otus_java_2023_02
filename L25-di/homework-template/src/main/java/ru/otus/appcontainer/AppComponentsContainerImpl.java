package ru.otus.appcontainer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;
import ru.otus.appcontainer.exception.IllegalComponentDefinitionException;
import ru.otus.appcontainer.utils.ClassLoaderUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class AppComponentsContainerImpl implements AppComponentsContainer {

    private final Map<Class<?>, Object> appComponentsByClass = new HashMap<>();
    private final Map<String, Object> appComponentsByName = new HashMap<>();

    public AppComponentsContainerImpl(Class<?> initialConfigClass) {
        processConfig(List.of(initialConfigClass));
    }

    public AppComponentsContainerImpl(Class<?>... initialConfigClasses) {
        processConfig(Arrays.stream(initialConfigClasses).toList());
    }

    public AppComponentsContainerImpl(String packageName) {
        List<Class<?>> configClasses = findAllConfigClasses(packageName);
        processConfig(configClasses);
    }

    private void processConfig(List<Class<?>> configClasses) {
        configClasses.forEach(this::checkConfigClass);
        Map<Method, Class<?>> componentMethods = configClasses.stream()
                .flatMap(cls ->
                        Arrays.stream(cls.getMethods())
                                .filter(m -> m.isAnnotationPresent(AppComponent.class))
                                .map(m -> new ImmutablePair<Method, Class<?>>(m, cls))
                )
                .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
        validateComponentMethods(componentMethods);

        List<Class<?>> configs = new ArrayList<>(configClasses);
        configs.sort((c1, c2) -> {
            AppComponentsContainerConfig a1 = c1.getAnnotation(AppComponentsContainerConfig.class);
            AppComponentsContainerConfig a2 = c2.getAnnotation(AppComponentsContainerConfig.class);
            return Integer.compare(a1.order(), a2.order());
        });
        configs.forEach(cls ->
                instantiateComponents(cls,
                        Arrays.stream(cls.getMethods())
                                .filter(m -> m.isAnnotationPresent(AppComponent.class))
                                .collect(Collectors.toList())
                )
        );
    }

    private void checkConfigClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(AppComponentsContainerConfig.class)) {
            throw new IllegalArgumentException(String.format("Given class is not config %s", configClass.getName()));
        }
    }

    private void validateComponentMethods(Map<Method, Class<?>> componentMethods) {
        HashSet<String> componentNames = new HashSet<>();
        HashMap<Class<?>, String> componentTypes = new HashMap<>();
        for (Method method : componentMethods.keySet()) {
            AppComponent annotation = method.getAnnotation(AppComponent.class);
            String componentName = annotation.name();
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new IllegalComponentDefinitionException(
                        String.format("Configuration method for component `%s` must have public modifier",
                                componentName)
                );
            }
            int componentOrder = annotation.order();
            if (componentOrder < 0) {
                throw new IllegalComponentDefinitionException(
                        String.format("Component `%s` cannot have negative order", componentName)
                );
            }
            if (componentNames.contains(componentName)) {
                throw new IllegalComponentDefinitionException(
                        String.format("Component `%s` is registered more than once", componentName)
                );
            }
            Class<?> componentType = method.getReturnType();
            if (componentTypes.containsKey(componentType)) {
                throw new IllegalArgumentException(
                        String.format("Configuration method for component `%s` returns already registered type `%s` for component `%s`",
                                componentName, componentType, componentTypes.get(componentType))
                );
            }
            componentNames.add(componentName);
            componentTypes.put(componentType, componentName);
        }
    }

    private void instantiateComponents(Class<?> configClass, List<Method> componentMethods) {
        Object configInstance = instantiateConfigClass(configClass);

        // Sorting the component methods in initialization order
        componentMethods.sort((m1, m2) -> {
            AppComponent appComponent1 = m1.getAnnotation(AppComponent.class);
            AppComponent appComponent2 = m2.getAnnotation(AppComponent.class);
            return Integer.compare(appComponent1.order(), appComponent2.order());
        });

        for (Method method : componentMethods) {
            AppComponent appComponent = method.getAnnotation(AppComponent.class);
            String componentName = appComponent.name();

            Object componentInstance = instantiateAppComponent(configInstance, componentName, method);
            appComponentsByName.put(componentName, componentInstance);
            appComponentsByClass.put(method.getReturnType(), componentInstance);
            appComponentsByClass.put(componentInstance.getClass(), componentInstance);
        }
    }

    private static Object instantiateConfigClass(Class<?> configClass) {
        try {
            Constructor<?> constructor = configClass.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    String.format("Configuration class `%s` must have no args constructor", configClass.getName()), e
            );
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Failed to instantiate configuration class %s", configClass.getName()), e
            );
        }
    }

    private Object instantiateAppComponent(Object configInstance,
                                           String componentName,
                                           Method methodComponent) {
        Object[] componentMethodArgs = new Object[methodComponent.getParameterCount()];
        Class<?>[] componentMethodParamTypes = methodComponent.getParameterTypes();
        for (int i = 0; i < componentMethodArgs.length; i++) {
            Class<?> clsType = componentMethodParamTypes[i];
            if (!appComponentsByClass.containsKey(clsType)) {
                throw new IllegalComponentDefinitionException(
                        String.format("Failed to instantiate component `%s` because the required component with type `%s` is not found",
                                componentName, clsType.getName())
                );
            }
            componentMethodArgs[i] = appComponentsByClass.get(clsType);
        }
        return createComponentInstance(configInstance, componentName, methodComponent, componentMethodArgs);
    }

    private Object createComponentInstance(Object configInstance,
                                           String componentName,
                                           Method componentMethod,
                                           Object... args) {
        try {
            return componentMethod.invoke(configInstance, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Failed to instantiate component `%s`", componentName), e);
        }
    }

    private List<Class<?>> findAllConfigClasses(String packageName) {
        try {
            return ClassLoaderUtils.findAllClasses(packageName)
                    .stream().filter(cls -> cls.isAnnotationPresent(AppComponentsContainerConfig.class))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to load classes for package %s", packageName), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C getAppComponent(Class<C> componentClass) {
        if (!appComponentsByClass.containsKey(componentClass)) {
            throw new IllegalArgumentException(
                    String.format("Component with type `%s` is not found", componentClass)
            );
        }
        return (C) appComponentsByClass.get(componentClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C getAppComponent(String componentName) {
        if (!appComponentsByName.containsKey(componentName)) {
            throw new IllegalArgumentException(
                    String.format("Component with name `%s` is not found", componentName)
            );
        }
        return (C) appComponentsByName.get(componentName);
    }
}
