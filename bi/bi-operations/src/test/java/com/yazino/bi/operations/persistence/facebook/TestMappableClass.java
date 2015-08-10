package com.yazino.bi.operations.persistence.facebook;

import java.util.Map;

import com.restfb.Facebook;

public class TestMappableClass {
    @Facebook
    private Map < Integer, String > mappedField;

    @Facebook
    private String stringField;

    public String getStringField() {
        return stringField;
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    @Facebook
    private Map oldStyleMappedField;

    public Map < Integer, String > getMappedField() {
        return mappedField;
    }
}
