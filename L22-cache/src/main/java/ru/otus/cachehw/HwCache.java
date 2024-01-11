package ru.otus.cachehw;


import java.util.Map;
import java.util.Set;

public interface HwCache<K, V> {

    void put(K key, V value);

    void remove(K key);

    V get(K key);

    Set<Map.Entry<K, V>> getEntries();

    void addListener(HwListener<K, V> listener);

    void removeListener(HwListener<K, V> listener);
}
