package ru.otus.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.core.cache.HwCache;
import ru.otus.core.cache.MyCache;
import ru.otus.core.repository.DataTemplate;
import ru.otus.core.sessionmanager.TransactionManager;
import ru.otus.crm.model.Client;

import java.util.List;
import java.util.Optional;

public class DbServiceClientImpl implements DBServiceClient {
    private static final Logger log = LoggerFactory.getLogger(DbServiceClientImpl.class);

    private final HwCache<Long, Client> cache = new MyCache<>();
    private final DataTemplate<Client> clientDataTemplate;
    private final TransactionManager transactionManager;

    public DbServiceClientImpl(TransactionManager transactionManager, DataTemplate<Client> clientDataTemplate) {
        this.transactionManager = transactionManager;
        this.clientDataTemplate = clientDataTemplate;
    }

    @Override
    public Client saveClient(Client client) {
        return transactionManager.doInTransaction(session -> {
            var clientCloned = client.clone();
            if (client.getId() == null) {
                var savedClient = clientDataTemplate.insert(session, clientCloned);
                log.info("created client: {}", clientCloned);
                cache.put(savedClient.getId(), savedClient);
                return savedClient;
            }
            var savedClient = clientDataTemplate.update(session, clientCloned);
            cache.put(savedClient.getId(), savedClient);
            log.info("updated client: {}", savedClient);
            return savedClient;
        });
    }

    @Override
    public Optional<Client> getClient(long id) {
        var client = cache.get(id);
        if (client != null) {
            return Optional.of(client);
        }

        return transactionManager.doInReadOnlyTransaction(session -> {
            var clientOptional = clientDataTemplate.findById(session, id);
            log.info("client: {}", clientOptional);
            return clientOptional;
        });
    }

    @Override
    public List<Client> findAll() {
        return transactionManager.doInReadOnlyTransaction(session -> {
            var clientList = clientDataTemplate.findAll(session);
            log.info("clientList:{}", clientList);
            return clientList;
       });
    }
}
