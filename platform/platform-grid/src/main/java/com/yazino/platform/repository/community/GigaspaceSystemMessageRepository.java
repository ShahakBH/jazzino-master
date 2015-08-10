package com.yazino.platform.repository.community;

import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.model.community.SystemMessage;
import com.yazino.platform.persistence.community.SystemMessageDAO;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspaceSystemMessageRepository implements SystemMessageRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceSystemMessageRepository.class);

    private final GigaSpace globalGigaSpace;
    private final SystemMessageDAO systemMessageDAO;

    @Autowired
    public GigaspaceSystemMessageRepository(@Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                            final SystemMessageDAO systemMessageDAO) {
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(systemMessageDAO, "System Message DAO may not be null");

        this.systemMessageDAO = systemMessageDAO;
        this.globalGigaSpace = globalGigaSpace;
    }


    @Override
    public List<SystemMessage> findValid() {
        LOG.debug("Finding all valid system messages");

        final Date currentTime = new Date();
        final SQLQuery<SystemMessage> messageQuery = new SQLQuery<SystemMessage>(
                SystemMessage.class, "validFrom <= ? and validTo >= ? order by validFrom desc",
                currentTime, currentTime);
        final SystemMessage[] validSystemMessages = globalGigaSpace.readMultiple(messageQuery, Integer.MAX_VALUE);

        if (validSystemMessages == null) {
            return Collections.emptyList();
        }

        final List<SystemMessage> systemMessageList
                = new ArrayList<SystemMessage>(Arrays.asList(validSystemMessages));
        LOG.debug("Found the following systemMessages: {}", systemMessageList);

        return systemMessageList;
    }

    @Override
    public void refreshSystemMessages() {
        LOG.debug("Loading all valid system messages into space");

        try {
            globalGigaSpace.clear(new SystemMessage());

            final Collection<SystemMessage> systemMessageFromDb = systemMessageDAO.findValid();
            if (systemMessageFromDb == null || systemMessageFromDb.isEmpty()) {
                LOG.debug("No system messages require loading into the space");
            } else {
                LOG.debug("Writing new system messages: {}", systemMessageFromDb);
                globalGigaSpace.writeMultiple(systemMessageFromDb.toArray(new SystemMessage[systemMessageFromDb.size()]));
            }

            LOG.debug("All valid system messages have been loaded into space");

        } catch (Throwable e) {
            LOG.error("System Message load to space failed", e);
        }
    }
}
