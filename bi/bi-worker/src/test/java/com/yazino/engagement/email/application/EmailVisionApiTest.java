package com.yazino.engagement.email.application;

import com.yazino.engagement.email.domain.EmailConfig;
import com.yazino.engagement.email.domain.EmailData;
import com.yazino.engagement.email.infrastructure.DayZeroEmailConfigService;
import com.yazino.engagement.email.infrastructure.EmailSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailVisionApiTest {

    private EmailApi underTest;
    @Mock
    private EmailSender emailSender;
    @Mock
    private DayZeroEmailConfigService emailConfigService;

    @Before
    public void setUp() throws Exception {
        underTest = new EmailVisionApi(emailConfigService, emailSender);
    }

    @Test
    public void sendDayZeroEmailShouldReturnFalseIfPropertySetToFalse() {
        assertFalse(underTest.sendDayZeroEmail(null));
        verifyZeroInteractions(emailSender);
    }

    @Test
    public void sendDayZeroEmailShouldCalLEmailSenderIfPropertySetToTrue() {
        when(emailConfigService.isDayZeroEmailEnabled()).thenReturn(true);
        when(emailSender.sendEmail(any(EmailConfig.class), any(EmailData.class))).thenReturn(true);

        assertTrue(underTest.sendDayZeroEmail(null));
    }
}

