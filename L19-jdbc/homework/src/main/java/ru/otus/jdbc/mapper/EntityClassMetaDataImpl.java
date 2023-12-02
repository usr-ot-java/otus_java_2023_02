package ru.otus.jdbc.mapper;

import ru.otus.core.annotation.Id;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityClassMetaDataImpl<T> implements EntityClassMetaData<T> {

    private final Class<T> clazz;
    private final Field idField;
    private final List<Field> allFields;
    private final List<Field> fieldsWithoutId = new ArrayList<>();
    private final Constructor<T> constructor;

    public EntityClassMetaDataImpl(Class<T> clazz) {
        this.clazz = clazz;
        this.allFields = Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toList());
        Field idField = null;
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
            } else {
                fieldsWithoutId.add(field);
            }
        }
        if (idField == null) {
            throw new IllegalStateException(
                    String.format("Class %s must have a field declared with @Id annotation", clazz.getName())
            );
        }
        this.idField = idField;
        try {
            Class<?>[] argTypes = allFields.stream().map(Field::getType).toArray(Class[]::new);
            this.constructor = clazz.getDeclaredConstructor(argTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    String.format("Class %s must have all fields constructor", clazz.getName()), e
            );
        }
    }

    @Override
    public String getName() {
        return clazz.getSimpleName();
    }

    @Override
    public Constructor<T> getConstructor() {
        return constructor;
    }

    @Override
    public Field getIdField() {
        return idField;
    }

    @Override
    public List<Field> getAllFields() {
        return allFields;
    }

    @Override
    public List<Field> getFieldsWithoutId() {
        return fieldsWithoutId;
    }

    @Override
    public Object extractFieldIdValue(T instance) {
        return extractFieldValue(instance, idField);
    }

    @Override
    public List<Object> extractFieldValuesWithoutId(T instance) {
        List<Field> fields = getFieldsWithoutId();
        List<Object> params = new ArrayList<>(getAllFields().size());
        for (Field field : fields) {
            Object value = extractFieldValue(instance, field);
            params.add(value);
        }
        return params;
    }

    private Object extractFieldValue(T instance, Field field) {
        boolean canAccess = field.canAccess(instance);
        if (!canAccess) {
            field.setAccessible(true);
        }

        Object value;
        try {
            value = field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    String.format("Cannot get the value of the field %s for the class %s", field.getName(), instance.getClass())
            );
        }

        if (!canAccess) {
            field.setAccessible(false);
        }
        return value;
    }

    @Override
    public Class<T> getEntityClass() {
        return clazz;
    }


}
