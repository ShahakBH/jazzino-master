package com.yazino.engagement.email.application;

import com.yazino.engagement.email.domain.EmailConfig;
import com.yazino.engagement.email.domain.EmailData;
import com.yazino.engagement.email.infrastructure.DayZeroEmailConfigService;
import com.yazino.engagement.email.infrastructure.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.yazino.engagement.email.infrastructure.DayZeroEmailConfigService.DAY_ZERO_EMAIL_ENABLED_KEY;

@Service
public class EmailVisionApi implements EmailApi {

    private static final Logger LOG = LoggerFactory.getLogger(EmailVisionApi.class);
    private final DayZeroEmailConfigService emailConfigService;
    private final EmailSender emailSender;

    @Autowired
    public EmailVisionApi(DayZeroEmailConfigService emailConfigService, EmailSender emailSender) {
        this.emailConfigService = emailConfigService;
        this.emailSender = emailSender;
    }

    @Override
    public boolean sendDayZeroEmail(EmailData emailData) {
        if (emailConfigService.isDayZeroEmailEnabled()) {
            EmailConfig emailConfig = emailConfigService.getEmailConfig();
            return emailSender.sendEmail(emailConfig, emailData);
        } else {
            LOG.info("Day Zero Email is turned off.Check the property {}", DAY_ZERO_EMAIL_ENABLED_KEY);
            return false;
        }
    }
}
