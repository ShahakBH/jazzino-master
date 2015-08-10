package com.yazino.platform.persistence.community;


import com.yazino.platform.model.community.SystemMessage;

import java.util.Collection;

public interface SystemMessageDAO {

    /**
     * Finds all currently valid system messages and return them.
     * <p/>
     * Currently valid means the valid to date is still in the future.
     *
     * @return the messages.
     */

    Collection<SystemMessage> findValid();

}
