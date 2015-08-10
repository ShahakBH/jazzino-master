package com.yazino.platform.service.community;

import com.yazino.platform.community.CommunityConfigurationUpdateService;
import com.yazino.platform.repository.community.SystemMessageRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import org.openspaces.remoting.RemotingService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingCommunityConfigurationUpdateService implements CommunityConfigurationUpdateService {
    private final SystemMessageRepository systemMessageRepository;
    private final TrophyRepository trophyRepository;

    @Autowired
    public GigaspaceRemotingCommunityConfigurationUpdateService(final SystemMessageRepository systemMessageRepository,
                                                                final TrophyRepository trophyRepository) {
        notNull(systemMessageRepository, "systemMessageRepository may not be null");
        notNull(trophyRepository, "trophyRepository may not be null");

        this.systemMessageRepository = systemMessageRepository;
        this.trophyRepository = trophyRepository;
    }

    @Override
    public void refreshSystemMessages() {
        systemMessageRepository.refreshSystemMessages();
    }

    @Override
    public void refreshTrophies() {
        trophyRepository.refreshTrophies();
    }
}
