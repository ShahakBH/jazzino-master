package com.yazino.platform.repository.table;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.model.table.Client;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaSpacesClientRepository implements ClientRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaSpacesClientRepository.class);

    private final GigaSpace globalGigaSpace;

    @Autowired
    public GigaSpacesClientRepository(@Qualifier("globalGigaSpace") final GigaSpace globalSpace) {
        notNull(globalSpace, "globalSpace may not be null");

        this.globalGigaSpace = globalSpace;
    }

    @Cacheable(cacheName = "clientByGameTypeCache")
    public Client[] findAll(final String gameType) {
        final Client template = new Client();
        template.setGameType(gameType);
        return globalGigaSpace.readMultiple(template, Integer.MAX_VALUE);
    }

    @Cacheable(cacheName = "clientByIdCache")
    public Client findById(final String clientId) {
        LOG.debug("Fetching Client by ID {}", clientId);
        return globalGigaSpace.readById(Client.class, clientId);
    }

}
