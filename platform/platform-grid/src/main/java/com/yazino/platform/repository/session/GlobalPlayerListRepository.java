package com.yazino.platform.repository.session;

import com.yazino.platform.model.session.GlobalPlayerList;

public interface GlobalPlayerListRepository {
    GlobalPlayerList lock();

    GlobalPlayerList read();

    void save(GlobalPlayerList list);
}
