package com.yazino.web.payment.googlecheckout.v3;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Produce a new globaly unique identifier.
 *
 * @return the identifier.
 */
@Component
public class JavaUUIDSource {
    public String getNewUUID() {
        return UUID.randomUUID().toString();
    }
}
