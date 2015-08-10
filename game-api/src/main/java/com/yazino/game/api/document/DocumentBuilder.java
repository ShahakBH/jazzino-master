package com.yazino.game.api.document;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A builder of documents, for a) convenience and b) to stop you putting objects of unsupported
 * types into documents.
 * <p/>
 * Do please try and avoid the complex collection methods - these are mostly here for backwards compatibility with the
 * older games. A simpler structure is a better structure.
 */
public class DocumentBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentBuilder.class);

    private static final Set<Class> OBJECT_WHITELIST = new HashSet<Class>();
    private static final Set<Class> ARRAY_WHITELIST = new HashSet<Class>();

    private final Map<String, Object> document = new HashMap<String, Object>();

    static {
        OBJECT_WHITELIST.add(String.class);
        OBJECT_WHITELIST.add(Double.class);
        OBJECT_WHITELIST.add(Float.class);
        OBJECT_WHITELIST.add(Long.class);
        OBJECT_WHITELIST.add(Integer.class);
        OBJECT_WHITELIST.add(Boolean.class);
        OBJECT_WHITELIST.add(BigDecimal.class);
        OBJECT_WHITELIST.add(HashMap.class);
        OBJECT_WHITELIST.add(TreeMap.class);
        OBJECT_WHITELIST.add(ArrayList.class);
        OBJECT_WHITELIST.add(HashSet.class);

        ARRAY_WHITELIST.add(int[].class);
        ARRAY_WHITELIST.add(long[].class);
        ARRAY_WHITELIST.add(float[].class);
        ARRAY_WHITELIST.add(double[].class);
        ARRAY_WHITELIST.add(boolean[].class);
    }

    public DocumentBuilder() {
    }

    public DocumentBuilder(final Map<String, Object> baseDocument) {
        if (baseDocument != null) {
            document.putAll(baseDocument);
        }
    }

    private DocumentBuilder withObject(final String key, final Object value) {
        notNull(key, "key may not be null");

        if (value != null) {
            document.put(key, value);
        } else {
            document.put(key, null);
        }

        return this;
    }

    public DocumentBuilder withString(final String key, final Object value) {
        notNull(key, "key may not be null");

        if (value != null) {
            document.put(key, value.toString());
        } else {
            document.put(key, null);
        }

        return this;
    }

    public DocumentBuilder withInt(final String key, final int value) {
        return withObject(key, value);
    }

    public DocumentBuilder withNullableInt(final String key, final Integer value) {
        return withObject(key, value);
    }

    public DocumentBuilder withLong(final String key, final long value) {
        return withObject(key, value);
    }

    public DocumentBuilder withNullableLong(final String key, final Long value) {
        return withObject(key, value);
    }

    public DocumentBuilder withBoolean(final String key, final boolean value) {
        return withObject(key, value);
    }

    public DocumentBuilder withNullableBoolean(final String key, final Boolean value) {
        return withObject(key, value);
    }

    public DocumentBuilder withBigDecimal(final String key, final BigDecimal value) {
        return withObject(key, value);
    }

    public DocumentBuilder withByteArray(final String key, final byte[] value){
        return withObject(key, value);
    }

    public <T extends Enum<T>> DocumentBuilder withEnum(final String key, final Enum<T> value) {
        if (value != null) {
            return withString(key, value.name());
        }
        return withString(key, null);
    }

    public DocumentBuilder withDate(final String key, final Date value) {
        return withObject(key, value);
    }

    public DocumentBuilder withStringArray(final String key, final String[] value) {
        return withObject(key, value);
    }

    public DocumentBuilder withIntArray(final String key, final int[] value) {
        return withObject(key, value);
    }

    public DocumentBuilder withLongArray(final String key, final int[] value) {
        return withObject(key, value);
    }

    public DocumentBuilder withBooleanArray(final String key, final int[] value) {
        return withObject(key, value);
    }

    public DocumentBuilder withBigDecimalArray(final String key, final BigDecimal[] value) {
        return withObject(key, value);
    }

    public DocumentBuilder withCollectionOfString(final String key, final Collection<String> value) {
        return withObject(key, value);
    }

    public DocumentBuilder withCollectionOfInteger(final String key, final Collection<Integer> value) {
        return withObject(key, value);
    }

    public DocumentBuilder withCollectionOfLong(final String key, final Collection<Long> value) {
        return withObject(key, value);
    }

    public DocumentBuilder withCollectionOfBoolean(final String key, final Collection<Boolean> value) {
        return withObject(key, value);
    }

    public DocumentBuilder withCollectionOfBigDecimal(final String key, final Collection<BigDecimal> value) {
        return withObject(key, value);
    }

    public DocumentBuilder withCollectionOfCollections(final String key, final Collection<?> value) {
        if (value != null) {
            if (LOG.isDebugEnabled()) {
                if (!validateCollection(value)) {
                    throw new IllegalArgumentException("Invalid class passed with class " + value.getClass().getName()
                            + " and value " + value);
                }
            }
            document.put(key, value);
        } else {
            document.put(key, null);
        }
        return this;
    }

    public DocumentBuilder withCollectionOfPrimitiveArray(final String key, final Collection<?> value) {
        if (value != null) {
            if (LOG.isDebugEnabled()) {
                verifyContentsOfPrimitiveArray(value);
            }
            document.put(key, value);
        } else {
            document.put(key, null);
        }
        return this;
    }

    private void verifyContentsOfPrimitiveArray(final Collection<?> value) {
        for (Object item : value) {
            if (item != null && !ARRAY_WHITELIST.contains(item.getClass())) {
                throw new IllegalArgumentException("Invalid class passed with class "
                        + item.getClass().getName() + " and value " + item);
            }
        }
    }

    public <T extends Enum<T>> DocumentBuilder withEnumArray(final String key, final Enum<T>[] value) {
        notNull(key, "key may not be null");

        if (value != null) {
            final String[] enumArray = new String[value.length];
            for (int i = 0; i < enumArray.length; i++) {
                enumArray[i] = ObjectUtils.toString(value[i]);
            }
            document.put(key, enumArray);
        } else {
            document.put(key, null);
        }

        return this;
    }

    public DocumentBuilder withSetOf(final String key, final Set<? extends Documentable> documentableSet) {
        notNull(key, "key may not be null");

        if (documentableSet != null) {
            final Set<Map<String, Object>> documents = new HashSet<Map<String, Object>>(documentableSet.size());
            for (Documentable documentable : documentableSet) {
                documents.add(documentable.toDocument());
            }
            document.put(key, documents);
        } else {
            document.put(key, null);
        }

        return this;
    }

    public DocumentBuilder withListOf(final String key, final List<? extends Documentable> documentableList) {
        return withCollectionOf(key, documentableList);
    }

    public DocumentBuilder withCollectionOf(final String key, final Collection<? extends Documentable> documentableList) {
        notNull(key, "key may not be null");

        if (documentableList != null) {
            final List<Map<String, Object>> documents = new ArrayList<Map<String, Object>>(documentableList.size());
            for (Documentable documentable : documentableList) {
                documents.add(documentable.toDocument());
            }
            document.put(key, documents);
        } else {
            document.put(key, null);
        }

        return this;
    }

    public <T extends Documentable> DocumentBuilder withListOf(final String key, final T[] documentableArray) {
        notNull(key, "key may not be null");

        if (documentableArray != null) {
            final List<Map<String, Object>> documents = new ArrayList<Map<String, Object>>(documentableArray.length);
            for (Documentable documentable : documentableArray) {
                documents.add(documentable.toDocument());
            }
            document.put(key, documents);
        } else {
            document.put(key, null);
        }

        return this;
    }

    public DocumentBuilder withMapOf(final String key, final Map<?, ? extends Documentable> documentableMap) {
        notNull(key, "key may not be null");

        if (documentableMap != null) {
            final Map<String, Map<String, Object>> documents = new HashMap<String, Map<String, Object>>(documentableMap.size());
            for (Object documentableKey : documentableMap.keySet()) {
                documents.put(documentableKey.toString(), documentableMap.get(documentableKey).toDocument());
            }
            document.put(key, documents);
        } else {
            document.put(key, null);
        }

        return this;
    }

    public DocumentBuilder withDocumentableMapOf(final String key, final Map<? extends Documentable, ? extends Documentable> documentableMap) {
        notNull(key, "key may not be null");

        if (documentableMap != null) {
            final Map<Map<String, Object>, Map<String, Object>> documents = new HashMap<Map<String, Object>, Map<String, Object>>(documentableMap.size());
            for (Documentable documentableKey : documentableMap.keySet()) {
                documents.put(documentableKey.toDocument(), documentableMap.get(documentableKey).toDocument());
            }
            document.put(key, documents);
        } else {
            document.put(key, null);
        }

        return this;
    }

    public DocumentBuilder withPrimitiveMapOf(final String key, final Map<?, ?> primitiveMap) {
        notNull(key, "key may not be null");

        if (LOG.isDebugEnabled()) {
            verifyContentsOfPrimitiveMap(key, primitiveMap);
        }

        return withObject(key, primitiveMap);
    }

    private void verifyContentsOfPrimitiveMap(final String key, final Map<?, ?> primitiveMap) {
        if (primitiveMap == null) {
            return;
        }

        for (Map.Entry<?, ?> entry : primitiveMap.entrySet()) {
            if (!OBJECT_WHITELIST.contains(entry.getKey().getClass())
                    || entry.getValue() != null && !OBJECT_WHITELIST.contains(entry.getValue().getClass())) {
                throw new IllegalArgumentException("Invalid class passed to map with key " + key + " : " + primitiveMap);
            }
        }
    }

    public DocumentBuilder withDocument(final String key, final Documentable documentable) {
        notNull(key, "key may not be null");

        if (documentable != null) {
            document.put(key, documentable.toDocument());
        } else {
            document.put(key, null);
        }

        return this;
    }

    public Map<String, Object> toDocument() {
        return document;
    }

    private boolean validateCollection(final Iterable<?> collection) {
        if (collection == null) {
            return true;
        }

        boolean valid = true;
        for (Object item : collection) {
            if (item instanceof Iterable) {
                valid &= validateCollection((Iterable) item);
            } else {
                valid &= item == null || OBJECT_WHITELIST.contains(item.getClass());
            }
        }
        return valid;
    }

}
