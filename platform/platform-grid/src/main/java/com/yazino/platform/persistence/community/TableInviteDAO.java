package com.yazino.platform.persistence.community;

import com.yazino.platform.model.community.TableInvite;

public interface TableInviteDAO {

    /**
     * Saves or updates given invite
     *
     * @param tableInvite
     */
    void save(TableInvite tableInvite);

}
