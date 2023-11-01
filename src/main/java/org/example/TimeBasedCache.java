package org.example;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

public class TimeBasedCache<K, V> extends ConcurrentSkipListMap<K, V> {
    private long maxAge;
    private TimeUnit unit;

    public TimeBasedCache(long maxAge, TimeUnit unit) {
        super();
        this.maxAge = maxAge;
        this.unit = unit;
    }

    @Override
    public V put(K key, V value) {
        removeExpiredEntries();
        return super.put(key, value);
    }

    private void removeExpiredEntries() {
        long currentTime = System.currentTimeMillis();

        for (Iterator<Entry<K, V>> it = this.entrySet().iterator(); it.hasNext(); ) {
            Entry<K, V> entry = it.next();
            K key = entry.getKey();

            if (currentTime - (Long)key > unit.toMillis(maxAge)) { // 这里的(Long)key代表了你的时间戳
                it.remove();
            }
        }
    }
}