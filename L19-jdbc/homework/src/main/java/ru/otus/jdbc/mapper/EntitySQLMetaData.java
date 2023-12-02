package ru.otus.jdbc.mapper;

/**
 * Создает SQL - запросы
 */
public interface EntitySQLMetaData<T> {
    EntityClassMetaData<T> getEntityClassMetaData();

    String getSelectAllSql();

    String getSelectByIdSql();

    String getInsertSql();

    String getUpdateSql();
}
