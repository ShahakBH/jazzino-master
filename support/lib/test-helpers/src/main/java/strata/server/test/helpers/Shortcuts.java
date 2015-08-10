package strata.server.test.helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Shortcuts needed to make tests a bit less verbose
 */
public final class Shortcuts {
    private Shortcuts() {
    }

    /**
     * @deprecated "Use Collections.singletonMap or Guava"
     */
    public static <K, V> Map<K, V> asMap(final Entry<K, V>... entries) {
        final Map<K, V> map = new HashMap<K, V>();
        for (final Entry<K, V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /**
     * @deprecated "use Pair from commons-lang3"
     */
    public static <K, V> Entry<K, V> kv(final K key, final V value) {
        return new SingleEntry<K, V>(key, value);
    }

    private static class SingleEntry<K, V> implements Entry<K, V> {
        private final K key;
        private V value;

        public SingleEntry(final K key, final V value) {
            super();
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(final V newValue) {
            this.value = newValue;
            return value;
        }

    }


    /**
     * @deprecated "use Sets.newHashSet(..) from Guava"
     */
    public static <T> Set<T> setOf(final T... entries) {
        final Set<T> set = new HashSet<T>();
        for (final T entry : entries) {
            set.add(entry);
        }
        return set;
    }

    public static <T extends Enum<?>> Set<T> setOfAll(final Class<T> clazz) {
        return setOf(new EnumAccessor<T>(clazz).values());
    }
}
