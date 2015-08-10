package com.yazino.game.api.document;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;

public final class DocumentAccessors {

    private DocumentAccessors() {
        // utility class
    }

    public static String stringFor(final Map<String, Object> document, final String key) {
        return (String) resolvePath(document, key);
    }

    public static String[] stringArrayFor(final Map<String, Object> document, final String key) {
        final String[] value = (String[]) resolvePath(document, key);
        if (value != null) {
            return value;
        }
        return new String[0];
    }

    public static Date dateFor(final Map<String, Object> document, final String key) {
        return (Date) resolvePath(document, key);
    }

    public static int intFor(final Map<String, Object> document, final String key) {
        final Integer value = (Integer) resolvePath(document, key);
        if (value != null) {
            return value;
        }
        return 0;
    }

    public static Integer nullableIntFor(final Map<String, Object> document, final String key) {
        return (Integer) resolvePath(document, key);
    }

    public static int[] intArrayFor(final Map<String, Object> document, final String key) {
        final int[] value = (int[]) resolvePath(document, key);
        if (value != null) {
            return value;
        }
        return new int[0];
    }

    public static long longFor(final Map<String, Object> document, final String key) {
        final Long value = (Long) resolvePath(document, key);
        if (value != null) {
            return value;
        }
        return 0L;
    }

    public static Long nullableLongFor(final Map<String, Object> document, final String key) {
        return (Long) resolvePath(document, key);
    }

    public static long[] longArrayFor(final Map<String, Object> document, final String key) {
        final long[] value = (long[]) resolvePath(document, key);
        if (value != null) {
            return value;
        }
        return new long[0];
    }

    public static boolean booleanFor(final Map<String, Object> document, final String key) {
        final Boolean value = (Boolean) resolvePath(document, key);
        if (value != null) {
            return value;
        }
        return false;
    }

    public static boolean[] booleanArrayFor(final Map<String, Object> document, final String key) {
        final boolean[] value = (boolean[]) resolvePath(document, key);
        if (value != null) {
            return value;
        }
        return new boolean[0];
    }

    public static Boolean nullableBooleanFor(final Map<String, Object> document, final String key) {
        return (Boolean) resolvePath(document, key);
    }

    public static BigDecimal bigDecimalFor(final Map<String, Object> document, final String key) {
        return (BigDecimal) resolvePath(document, key);
    }

    public static BigDecimal[] bigDecimalArrayFor(final Map<String, Object> document, final String key) {
        final BigDecimal[] value = (BigDecimal[]) resolvePath(document, key);
        if (value != null) {
            return value;
        }
        return new BigDecimal[0];
    }

    public static <T extends Enum<T>> T enumFor(final Map<String, Object> document, final Class<T> enumClass, final String key) {
        final String enumId = stringFor(document, key);
        if (enumId != null && !enumId.trim().isEmpty()) {
            return T.valueOf(enumClass, enumId);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T[] enumArrayFor(final Map<String, Object> document, final Class<T> enumClass, final String key) {
        final String[] value = (String[]) resolvePath(document, key);
        if (value != null) {
            final T[] enumArray = (T[]) Array.newInstance(enumClass, value.length);
            for (int i = 0; i < enumArray.length; i++) {
                if (value[i] != null) {
                    enumArray[i] = T.valueOf(enumClass, value[i]);
                }
            }
            return enumArray;
        }
        return (T[]) Array.newInstance(enumClass, 0);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> documentFor(final Map<String, Object> document, final String key) {
        final Map<String, Object> resolvedDocument = (Map<String, Object>) resolvePath(document, key);
        if (resolvedDocument != null) {
            return resolvedDocument;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static Set<Map<String, Object>> setOfDocumentsFor(final Map<String, Object> document, final String key) {
        final Set<Map<String, Object>> set = (Set<Map<String, Object>>) resolvePath(document, key);
        if (set != null) {
            return set;
        }
        return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> listOfDocumentsFor(final Map<String, Object> document, final String key) {
        final List<Map<String, Object>> list = (List<Map<String, Object>>) resolvePath(document, key);
        if (list != null) {
            return list;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static Collection<Map<String, Object>> collectionOfDocumentsFor(final Map<String, Object> document, final String key) {
        final Collection<Map<String, Object>> collection = (Collection<Map<String, Object>>) resolvePath(document, key);
        if (collection != null) {
            return collection;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Object>> mapOfDocumentsFor(final Map<String, Object> document, final String key) {
        final Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) resolvePath(document, key);
        if (map != null) {
            return map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static Map<Map<String, Object>, Map<String, Object>> documentableMapOfDocumentsFor(final Map<String, Object> document, final String key) {
        final Map<Map<String, Object>, Map<String, Object>> map = (Map<Map<String, Object>, Map<String, Object>>) resolvePath(document, key);
        if (map != null) {
            return map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private static Object resolvePath(final Map<String, Object> parent, final String keyPath) {
        Object currentElement = parent;
        for (String pathElement : keyPath.split("\\.")) {
            if (currentElement == null) {
                currentElement = Collections.<String, Object>emptyMap();
            }
            currentElement = ((Map<String, Object>) currentElement).get(pathElement);
        }
        return currentElement;
    }

}
