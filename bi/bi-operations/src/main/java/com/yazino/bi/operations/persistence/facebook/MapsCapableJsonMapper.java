package com.yazino.bi.operations.persistence.facebook;

import com.restfb.DefaultJsonMapper;
import com.restfb.Facebook;
import com.restfb.exception.FacebookJsonMappingException;
import com.restfb.json.JsonObject;
import com.restfb.util.ReflectionUtils.FieldWithAnnotation;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static com.restfb.util.ReflectionUtils.getFirstParameterizedTypeArgument;
import static com.restfb.util.StringUtils.trimToEmpty;

/**
 * Extends the default JSON mapper functionality in order to be able to map JSON map objects
 */
public class MapsCapableJsonMapper extends DefaultJsonMapper {
    /**
     * Maps the JSON string to a map
     *
     * @param <K>        Key's Java type
     * @param <V>        Value's Java type
     * @param jsonSource JSON source string
     * @param keyType    Key's Java type
     * @param valueType  Value's Java type
     * @return Map created from the JSON object
     */
    private <K, V> Map<K, V> toJavaMap(final String jsonSource, final Class<K> keyType,
                                       final Class<V> valueType) {
        if (keyType == null || valueType == null) {
            throw new FacebookJsonMappingException("Both type arguments must be specified when mapping a map");
        }
        final String json = trimToEmpty(jsonSource);

        final Map<K, V> retval = new HashMap<K, V>();
        final JsonObject jsonObject = new JsonObject(json);
        for (final String key : JsonObject.getNames(jsonObject)) {
            retval.put(toJavaObject(key, keyType), toJavaObject(jsonObject.getString(key), valueType));
        }
        return retval;
    }

    @Override
    protected Object toJavaType(final FieldWithAnnotation<Facebook> fieldWithAnnotation,
                                final JsonObject jsonObject, final String facebookFieldName) {
        final Class<?> type = fieldWithAnnotation.getField().getType();
        final Object rawValue = jsonObject.get(facebookFieldName);
        if (Map.class.equals(type)) {
            return toJavaMap(rawValue.toString(), getFirstParameterizedTypeArgument(fieldWithAnnotation.getField()),
                    getSecondParameterizedTypeArgument(fieldWithAnnotation.getField()));
        }
        return super.toJavaType(fieldWithAnnotation, jsonObject, facebookFieldName);
    }

    /**
     * Gets the second generic class parameter for a field
     *
     * @param field Field object
     * @return Returns the second field type's type argument
     */
    private Class<?> getSecondParameterizedTypeArgument(final Field field) {
        final Type type = field.getGenericType();
        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final Type firstTypeArgument = parameterizedType.getActualTypeArguments()[1];
        return (Class<?>) firstTypeArgument;
    }
}
