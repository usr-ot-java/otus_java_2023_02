package ru.otus.jdbc.mapper;

import ru.otus.core.repository.DataTemplate;
import ru.otus.core.repository.executor.DbExecutor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Сохратяет объект в базу, читает объект из базы
 */
public class DataTemplateJdbc<T> implements DataTemplate<T> {

    private final DbExecutor dbExecutor;
    private final EntitySQLMetaData<T> entitySQLMetaData;

    public DataTemplateJdbc(DbExecutor dbExecutor, EntitySQLMetaData<T> entitySQLMetaData) {
        this.dbExecutor = dbExecutor;
        this.entitySQLMetaData = entitySQLMetaData;
    }

    @Override
    public Optional<T> findById(Connection connection, long id) {
        return dbExecutor.executeSelect(connection, entitySQLMetaData.getSelectByIdSql(), List.of(id), (res) -> {
            try {
                if (!res.next()) {
                    return null;
                }

                Object[] args = getInstanceArgs(res);
                return newInstance(args);
            } catch (SQLException e) {
                throw new IllegalStateException(
                        String.format("Failed to get data from the database for the class %s", entitySQLMetaData.getEntityClassMetaData().getEntityClass().getName()),
                        e
                );
            }
        });
    }

    @Override
    public List<T> findAll(Connection connection) {
        return dbExecutor.executeSelect(connection, entitySQLMetaData.getSelectAllSql(), Collections.emptyList(), (res) -> {
           List<T> result = new ArrayList<>();
           try {
               while (res.next()) {
                   Object[] args = getInstanceArgs(res);
                   T instance = newInstance(args);
                   result.add(instance);
               }
           } catch (SQLException e) {
               throw new IllegalStateException(
                       String.format("Failed to get data from the database for the class %s", entitySQLMetaData.getEntityClassMetaData().getEntityClass().getName()),
                       e
               );
           }
           return result;
        }).orElse(new ArrayList<>());
    }

    @Override
    public long insert(Connection connection, T client) {
        return dbExecutor.executeStatement(connection,
                entitySQLMetaData.getInsertSql(),
                entitySQLMetaData.getEntityClassMetaData().extractFieldValuesWithoutId(client)
        );
    }

    @Override
    public void update(Connection connection, T client) {
        List<Object> argsWithoutId = entitySQLMetaData.getEntityClassMetaData().extractFieldValuesWithoutId(client);
        List<Object> args = new ArrayList<>(argsWithoutId.size() + 1);
        args.addAll(argsWithoutId);
        args.add(entitySQLMetaData.getEntityClassMetaData().extractFieldIdValue(client));
        dbExecutor.executeStatement(connection, entitySQLMetaData.getUpdateSql(), args);
    }

    private Object[] getInstanceArgs(ResultSet res) throws SQLException {
        List<Field> allFields = entitySQLMetaData.getEntityClassMetaData().getAllFields();
        Object[] args = new Object[allFields.size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = res.getObject(allFields.get(i).getName().toLowerCase());
        }
        return args;
    }

    private T newInstance(Object[] args) {
        try {
            return entitySQLMetaData.getEntityClassMetaData().getConstructor().newInstance(args);
        }  catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(
                    String.format("Failed to create instance for the class %s", entitySQLMetaData.getEntityClassMetaData().getEntityClass().getName()),
                    e
            );
        }
    }
}
