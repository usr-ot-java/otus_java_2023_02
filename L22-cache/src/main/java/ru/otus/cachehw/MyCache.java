package ru.otus.cachehw;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class MyCache<K, V> implements HwCache<K, V> {
    private final WeakHashMap<K, V> storage = new WeakHashMap<>();
    private final Set<HwListener<K, V>> listeners = new HashSet<>();

    @Override
    public void put(K key, V value) {
        storage.put(key, value);
        notifyListeners(CacheEvent.PUT, key, value);
    }

    @Override
    public void remove(K key) {
        V value = storage.remove(key);
        notifyListeners(CacheEvent.REMOVE, key, value);
    }

    @Override
    public V get(K key) {
        V value = storage.get(key);
        notifyListeners(CacheEvent.GET, key, value);
        return value;
    }

    @Override
    public Set<Map.Entry<K, V>> getEntries() {
        Set<Map.Entry<K, V>> entries = storage.entrySet();
        entries.forEach(e -> notifyListeners(CacheEvent.GET, e.getKey(), e.getValue()));
        return entries;
    }

    @Override
    public void addListener(HwListener<K, V> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(HwListener<K, V> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(CacheEvent event, K key, V value) {
        for (var listener : listeners) {
            try {
                listener.notify(key, value, event.getEvent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
