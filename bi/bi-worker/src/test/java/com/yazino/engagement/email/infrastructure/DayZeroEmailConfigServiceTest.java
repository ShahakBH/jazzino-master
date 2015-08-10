package com.yazino.engagement.email.infrastructure;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.email.domain.EmailConfig;
import com.yazino.engagement.email.domain.SynchronizationType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DayZeroEmailConfigServiceTest {

    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private DayZeroEmailConfigService underTest;

    private static final String encryptValue = " abcd!!1234";
    private static final String randomValue = " z1x2c3v4b5";

    @Before
    public void setUp() throws Exception {
        underTest = new DayZeroEmailConfigService(encryptValue, randomValue, yazinoConfiguration);
    }

    @Test
    public void getEmailConfigServiceShouldUseYazinoConfigToSetEmailParameters() {

        EmailConfig expectedEmailConfig = new EmailConfig(encryptValue, randomValue, "email",
                SynchronizationType.NOTHING);
        assertThat(underTest.getEmailConfig(), is(equalTo(expectedEmailConfig)));

    }

    @Test
    public void isDayZeroEmailEnabledShouldReturnTrueIfItIsEnabled() {
        when(yazinoConfiguration.getBoolean(DayZeroEmailConfigService.DAY_ZERO_EMAIL_ENABLED_KEY)).thenReturn(true);
        assertTrue(underTest.isDayZeroEmailEnabled());
    }

    @Test
    public void isDayZeroEmailEnabledShouldReturnFalseIfItIsNotEnabled() {
        assertFalse(underTest.isDayZeroEmailEnabled());
    }
}
