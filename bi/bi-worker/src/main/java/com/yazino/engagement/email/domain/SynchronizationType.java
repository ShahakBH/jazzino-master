package com.yazino.engagement.email.domain;

/**
 * Synchronization type to be specified in transactional message
 */
public enum SynchronizationType {
    /**
     * The member table will not be synchronized
     */
    NOTHING,
    /**
     * The new profile will be inserted with all the associated DYN data in the member table
     */
    INSERT,
    /**
     * The profile and all the associated DYN data will be updated in the member table
     */
    UPDATE,
    /**
     * The system will try to update the profile in the member table.
     * If the update fails, it tries to insert the profile with the associated DYN data into the member table.
     */
    INSERT_UPDATE
}
