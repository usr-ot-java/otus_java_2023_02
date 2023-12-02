package ru.otus.jdbc.mapper;

import org.junit.jupiter.api.Test;
import ru.otus.crm.model.Client;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntitySQLMetaDataTest {

    private static final EntityClassMetaData<Client> entityClassMetaDataClient = new EntityClassMetaDataImpl<>(Client.class);
    private static final EntitySQLMetaData<Client> entitySQLMetaDataClient = new EntitySQLMetaDataImpl<>(entityClassMetaDataClient);

    @Test
    public void testGetSelectAllSql() {
        String result = entitySQLMetaDataClient.getSelectAllSql();
        assertEquals("SELECT id, name FROM client", result);
    }

    @Test
    public void testGetSelectByIdSql() {
        String result = entitySQLMetaDataClient.getSelectByIdSql();
        assertEquals("SELECT id, name FROM client WHERE id = ?", result);
    }

    @Test
    public void testGetInsertSql() {
        String result = entitySQLMetaDataClient.getInsertSql();
        assertEquals("INSERT INTO client (id, name) VALUES (?, ?)", result);
    }

    @Test
    public void testGetUpdateSql() {
        String result = entitySQLMetaDataClient.getUpdateSql();
        assertEquals("UPDATE client SET name = ? WHERE id = ?", result);
    }

}
