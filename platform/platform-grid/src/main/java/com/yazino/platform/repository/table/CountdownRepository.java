package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Countdown;

import java.util.Collection;

public interface CountdownRepository {

    Collection<Countdown> find();

    Countdown find(String countdownId);

    void removeCountdownFromSpace(Countdown countdown);

    void publishIntoSpace(Countdown countdown);
}
