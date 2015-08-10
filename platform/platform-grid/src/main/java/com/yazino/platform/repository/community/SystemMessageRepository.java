package com.yazino.platform.repository.community;


import com.yazino.platform.model.community.SystemMessage;

import java.util.List;

public interface SystemMessageRepository {

    /**
     * Find all currently valid system messages.
     *
     * @return all currently valid system message sorted by validFrom desc.
     */
    List<SystemMessage> findValid();

    /**
     * Load all currently valid system messages in the database into the space.
     */
    void refreshSystemMessages();
}
