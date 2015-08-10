package com.yazino.platform.persistence.community;


import com.yazino.platform.community.Trophy;

import java.util.Collection;

public interface TrophyDAO {

    void save(Trophy trophy);

    Collection<Trophy> retrieveAll();

}
