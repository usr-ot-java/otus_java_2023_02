package ru.otus.jdbc.mapper;

import java.util.stream.Collectors;

public class EntitySQLMetaDataImpl<T> implements EntitySQLMetaData<T> {

    private final EntityClassMetaData<T> entityClassMetaData;

    public EntitySQLMetaDataImpl(EntityClassMetaData<T> entityClassMetaData) {
        this.entityClassMetaData = entityClassMetaData;
    }

    @Override
    public EntityClassMetaData<T> getEntityClassMetaData() {
        return entityClassMetaData;
    }

    @Override
    public String getSelectAllSql() {
        return String.format("SELECT %s FROM %s",
                joinAllFieldsInLowercase(),
                entityClassMetaData.getName().toLowerCase()
        );
    }

    @Override
    public String getSelectByIdSql() {
        return String.format("SELECT %s FROM %s WHERE %s = ?",
                joinAllFieldsInLowercase(),
                entityClassMetaData.getName().toLowerCase(),
                entityClassMetaData.getIdField().getName()
        );
    }

    @Override
    public String getInsertSql() {
        return String.format("INSERT INTO %s (%s) VALUES (%s)",
                entityClassMetaData.getName().toLowerCase(),
                entityClassMetaData.getFieldsWithoutId().stream().map(f -> f.getName().toLowerCase()).collect(Collectors.joining(", ")),
                entityClassMetaData.getFieldsWithoutId().stream().map(f -> "?").collect(Collectors.joining(", "))
        );
    }

    @Override
    public String getUpdateSql() {
        return String.format("UPDATE %s SET %s WHERE %s = ?",
                entityClassMetaData.getName().toLowerCase(),
                entityClassMetaData.getFieldsWithoutId().stream().map(f -> f.getName().toLowerCase() + " = ?").collect(Collectors.joining(", ")),
                entityClassMetaData.getIdField().getName().toLowerCase()
        );
    }

    private String joinAllFieldsInLowercase() {
        return entityClassMetaData.getAllFields().stream()
                .map(f -> f.getName().toLowerCase())
                .collect(Collectors.joining(", "));
    }
}
