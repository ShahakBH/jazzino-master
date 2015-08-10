package com.yazino.platform.service.community;

import com.yazino.platform.community.ProfanityService;
import com.yazino.platform.persistence.community.BadWordDAO;
import org.openspaces.remoting.RemotingService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingProfanityService implements ProfanityService {
    private final BadWordDAO badWordDAO;

    @Autowired
    public GigaspaceRemotingProfanityService(final BadWordDAO badWordDAO) {
        notNull(badWordDAO, "badWordDAO may not be null");

        this.badWordDAO = badWordDAO;
    }

    @Override
    public Set<String> findAllProhibitedWords() {
        return badWordDAO.findAllBadWords();
    }

    @Override
    public Set<String> findAllProhibitedPartWords() {
        return badWordDAO.findAllPartBadWords();
    }

}
