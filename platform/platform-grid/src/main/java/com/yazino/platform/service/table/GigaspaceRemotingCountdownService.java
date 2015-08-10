package com.yazino.platform.service.table;


import com.yazino.platform.model.table.Countdown;
import com.yazino.platform.repository.table.CountdownRepository;
import com.yazino.platform.table.CountdownService;
import org.openspaces.remoting.RemotingService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingCountdownService implements CountdownService {

    private final CountdownRepository countdownRepository;

    @Autowired
    public GigaspaceRemotingCountdownService(final CountdownRepository countdownRepository) {
        notNull(countdownRepository, "countdownRepository may not be null");

        this.countdownRepository = countdownRepository;
    }

    @Override
    public Map<String, Long> findAll() {
        final Collection<Countdown> countdowns = countdownRepository.find();
        final Map<String, Long> countdownIds = new HashMap<String, Long>();
        for (Countdown countdown : countdowns) {
            countdownIds.put(countdown.getId(), countdown.getCountdown());
        }
        return countdownIds;
    }

}
