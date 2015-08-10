package com.yazino.mobile.yaps.config;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Map;

public class TypedMapBean<K, V> {

    private final Map<K, V> mSource;

    public TypedMapBean(Map<K, V> source) {
        Validate.notNull(source);
        mSource = source;
    }

    public Map<K, V> getSource() {
        return mSource;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).
                append("source", mSource).
                toString();
    }
}
