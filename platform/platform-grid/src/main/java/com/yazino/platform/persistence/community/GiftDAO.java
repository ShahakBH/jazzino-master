package com.yazino.platform.persistence.community;

import com.yazino.platform.model.community.Gift;
import com.yazino.platform.persistence.DataIterable;

public interface GiftDAO extends DataIterable<Gift> {

    void save(Gift gift);

}
