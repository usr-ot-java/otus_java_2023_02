package ru.otus.appcontainer;

import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;
import ru.otus.appcontainer.exception.IllegalComponentDefinitionException;

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
        processConfig(initialConfigClass);
    }

    private void processConfig(Class<?> configClass) {
        checkConfigClass(configClass);
        List<Method> componentMethods = Arrays.stream(configClass.getMethods())
                .filter(m -> m.isAnnotationPresent(AppComponent.class))
                .collect(Collectors.toList());
        validateComponentMethods(componentMethods);
        instantiateComponents(configClass, componentMethods);
    }

    private void checkConfigClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(AppComponentsContainerConfig.class)) {
            throw new IllegalArgumentException(String.format("Given class is not config %s", configClass.getName()));
        }
    }

    private void validateComponentMethods(List<Method> componentMethods) {
        HashSet<String> componentNames = new HashSet<>();
        HashMap<Class<?>, String> componentTypes = new HashMap<>();
        for (Method method : componentMethods) {
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
        Object configInstance;
        try {
            Constructor<?> constructor = configClass.getDeclaredConstructor();
            configInstance = constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    String.format("Configuration class `%s` must have no args constructor", configClass.getName()), e
            );
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Failed to instantiate configuration class %s", configClass.getName()), e
            );
        }

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
