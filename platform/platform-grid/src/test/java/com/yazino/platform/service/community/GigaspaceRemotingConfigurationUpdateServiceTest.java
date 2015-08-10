package com.yazino.platform.service.community;

import com.yazino.platform.repository.community.SystemMessageRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class GigaspaceRemotingConfigurationUpdateServiceTest {

    @Mock
    private SystemMessageRepository systemMessageRepository;
    @Mock
    private TrophyRepository trophyRepository;

    private GigaspaceRemotingCommunityConfigurationUpdateService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new GigaspaceRemotingCommunityConfigurationUpdateService(
                systemMessageRepository, trophyRepository);
    }

    @Test
    public void systemMessageRefreshIsProcessedByTheRepository() {
        underTest.refreshSystemMessages();

        verify(systemMessageRepository).refreshSystemMessages();
    }

    @Test
    public void trophyRefreshIsProcessedByTheRepository() {
        underTest.refreshTrophies();

        verify(trophyRepository).refreshTrophies();
    }

}
