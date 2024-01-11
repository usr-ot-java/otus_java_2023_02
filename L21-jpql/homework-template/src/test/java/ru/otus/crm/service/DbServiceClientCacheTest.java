package ru.otus.crm.service;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.otus.core.repository.DataTemplateHibernate;
import ru.otus.core.sessionmanager.TransactionManagerHibernate;
import ru.otus.crm.model.Address;
import ru.otus.crm.model.Client;
import ru.otus.crm.model.Phone;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Демо работы с hibernate (с абстракциями) должно ")
class DbServiceClientCacheTest {

    private StandardServiceRegistry serviceRegistry;
    private Metadata metadata;
    private SessionFactory sessionFactory;
    private TransactionManagerHibernate transactionManager;
    private DataTemplateHibernate<Client> clientTemplate;
    private DBServiceClient dbServiceClient;

    @BeforeEach
    public void setUp() {
        makeTestDependencies();
    }

    @AfterEach
    public void tearDown() {
        sessionFactory.close();
    }

    @Test
    @DisplayName("Клиент должен быть сохранён в кэш после вставки в БД")
    void shouldCorrectSaveClient() {
        //given
        var client = new Client(null, "Vasya", new Address(null, "AnyStreet"), List.of(new Phone(null, "13-555-22"),
                new Phone(null, "14-666-333")));
        var savedClient = dbServiceClient.saveClient(client);
        System.out.println(savedClient);

        //when
        // Verify that the value is taken from the cache
        applyCustomSqlStatementLogger(new SqlStatementLogger(true, true, false, 0) {
            @Override
            public void logStatement(String statement) {
                super.logStatement(statement);
                assertThat(statement).doesNotContain("select");
            }
        });
        var loadedSavedClient = dbServiceClient.getClient(savedClient.getId());

        //then
        assertThat(loadedSavedClient).isPresent();
        assertThat(loadedSavedClient).get()
                .usingRecursiveComparison().isEqualTo(savedClient);
    }

    @Test
    @DisplayName("Клиент должен быть сохранён в кэш после обновления в БД")
    void shouldCorrectUpdateClient() {
        //given
        var client = new Client(null, "Vasya", new Address(null, "AnyStreet"), List.of(new Phone(null, "13-555-22"),
                new Phone(null, "14-666-333")));

        var savedClient = dbServiceClient.saveClient(client);
        System.out.println(savedClient);

        savedClient.setName("New name");
        dbServiceClient.saveClient(savedClient);

        //when
        // Verify that the value is taken from the cache
        applyCustomSqlStatementLogger(new SqlStatementLogger(true, true, false, 0) {
            @Override
            public void logStatement(String statement) {
                super.logStatement(statement);
                assertThat(statement).doesNotContain("select");
            }
        });
        var loadedSavedClient = dbServiceClient.getClient(savedClient.getId());

        //then
        assertThat(loadedSavedClient).isPresent();
        assertThat(loadedSavedClient).get()
                .usingRecursiveComparison().isEqualTo(savedClient);
    }

    private void makeTestDependencies() {
        var cfg = new Configuration();

        cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");

        cfg.setProperty("hibernate.connection.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");

        cfg.setProperty("hibernate.connection.username", "sa");
        cfg.setProperty("hibernate.connection.password", "");

        cfg.setProperty("hibernate.show_sql", "true");
        cfg.setProperty("hibernate.format_sql", "false");
        cfg.setProperty("hibernate.generate_statistics", "true");

        cfg.setProperty("hibernate.hbm2ddl.auto", "create");
        cfg.setProperty("hibernate.enable_lazy_load_no_trans", "false");

        serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(cfg.getProperties()).build();


        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        metadataSources.addAnnotatedClass(Phone.class);
        metadataSources.addAnnotatedClass(Address.class);
        metadataSources.addAnnotatedClass(Client.class);
        metadata = metadataSources.getMetadataBuilder().build();
        sessionFactory = metadata.getSessionFactoryBuilder().build();
        transactionManager = new TransactionManagerHibernate(sessionFactory);
        clientTemplate = new DataTemplateHibernate<>(Client.class);
        dbServiceClient = new DbServiceClientImpl(transactionManager, clientTemplate);
    }

    private void applyCustomSqlStatementLogger(SqlStatementLogger customSqlStatementLogger) {
        var jdbcServices = serviceRegistry.getService(JdbcServices.class);
        try {
            Field field = jdbcServices.getClass().getDeclaredField("sqlStatementLogger");
            field.setAccessible(true);
            field.set(jdbcServices, customSqlStatementLogger);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}