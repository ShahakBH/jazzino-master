package com.yazino.platform.util;


import java.util.UUID;

/**
 * This implementation relies on the Java standard library to produce UUIDs.
 */
public class JavaUUIDSource implements UUIDSource {


    @Override
    public String getNewUUID() {
        return UUID.randomUUID().toString();
    }
}
