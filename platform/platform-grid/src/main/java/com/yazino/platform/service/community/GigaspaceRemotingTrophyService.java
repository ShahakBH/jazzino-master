package com.yazino.platform.service.community;


import com.yazino.platform.community.Trophy;
import com.yazino.platform.community.TrophyService;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.community.TrophyRepository;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingTrophyService implements TrophyService {

    private final TrophyRepository trophyRepository;
    private final SequenceGenerator sequenceGenerator;

    @Autowired
    public GigaspaceRemotingTrophyService(final TrophyRepository trophyRepository,
                                          final SequenceGenerator sequenceGenerator) {
        notNull(trophyRepository, "trophyRepository may not be null");
        notNull(sequenceGenerator, "sequenceGenerator may not be null");

        this.trophyRepository = trophyRepository;
        this.sequenceGenerator = sequenceGenerator;
    }


    @Override
    public List<Trophy> findAll() {
        final List<Trophy> trophies = trophyRepository.findAll();
        if (trophies != null) {
            return trophies;
        }
        return Collections.emptyList();
    }

    @Override
    public Trophy findById(@Routing final BigDecimal id) {
        notNull(id, "id may not be null");

        return trophyRepository.findById(id);
    }

    @Override
    public void update(@Routing("getId") final Trophy trophy) {
        notNull(trophy, "trophy may not be null");

        trophyRepository.save(trophy);
    }


    @Override
    public BigDecimal create(final Trophy trophy) {
        notNull(trophy, "trophy may not be null");

        final BigDecimal id = sequenceGenerator.next();
        trophy.setId(id);

        trophyRepository.save(trophy);

        return id;
    }

    @Override
    public List<Trophy> findForGameType(final String gameType) {
        notNull(gameType, "gameType may not be null");

        return new LinkedList<Trophy>(trophyRepository.findForGameType(gameType));
    }

}
