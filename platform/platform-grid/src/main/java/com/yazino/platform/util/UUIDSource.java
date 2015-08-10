package com.yazino.platform.util;


public interface UUIDSource {

    /**
     * Produce a new globaly unique identifier.
     *
     * @return the identifier.
     */

    String getNewUUID();

}
