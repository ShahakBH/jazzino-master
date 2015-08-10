package com.yazino.platform.repository.session;

import com.gigaspaces.client.ReadModifiers;
import com.yazino.platform.model.session.GlobalPlayerList;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ConcurrentModificationException;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspacesGlobalPlayerListRepository implements GlobalPlayerListRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspacesGlobalPlayerListRepository.class);

    private final GigaSpace globalGigaSpace;

    @Autowired
    public GigaspacesGlobalPlayerListRepository(@Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace) {
        notNull(globalGigaSpace, "globalGigaSpace may not be null");

        this.globalGigaSpace = globalGigaSpace;
    }

    @Override
    public GlobalPlayerList lock() {
        initialiseIfRequired();

        final GlobalPlayerList result = globalGigaSpace.readById(GlobalPlayerList.class, GlobalPlayerList.DEFAULT_ID,
                GlobalPlayerList.DEFAULT_ID, 0, ReadModifiers.EXCLUSIVE_READ_LOCK);
        if (result == null) {
            throw new ConcurrentModificationException("Cannot obtain lock");
        }
        return result;
    }

    @Override
    public GlobalPlayerList read() {
        return initialiseIfRequired();
    }

    @Override
    public void save(final GlobalPlayerList list) {
        globalGigaSpace.write(list);
    }

    private GlobalPlayerList initialiseIfRequired() {
        GlobalPlayerList gpl = globalGigaSpace.readById(GlobalPlayerList.class, GlobalPlayerList.DEFAULT_ID,
                GlobalPlayerList.DEFAULT_ID, 0, ReadModifiers.DIRTY_READ);
        if (gpl == null) {
            LOG.debug("Initialising Global Player List");

            gpl = new GlobalPlayerList();
            globalGigaSpace.write(gpl);
        }
        return gpl;
    }
}
