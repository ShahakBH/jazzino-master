package com.yazino.engagement.email.infrastructure;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.email.domain.EmailConfig;
import com.yazino.engagement.email.domain.SynchronizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DayZeroEmailConfigService {

    private static final String UID_KEY = "email";
    public static final String DAY_ZERO_EMAIL_ENABLED_KEY = "emailvision.dayzero.enabled";

    private final YazinoConfiguration yazinoConfiguration;

    private final String encryptValue;
    private final String randomValue;

    @Autowired
    public DayZeroEmailConfigService(@Value("${emailvision.dayzero.encryptvalue}") String encryptValue,
                                     @Value("${emailvision.dayzero.randomvalue}") String randomValue,
                                     YazinoConfiguration yazinoConfiguration) {
        this.encryptValue = encryptValue;
        this.randomValue = randomValue;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public boolean isDayZeroEmailEnabled() {
        return (yazinoConfiguration.getBoolean(DAY_ZERO_EMAIL_ENABLED_KEY));
    }

    public EmailConfig getEmailConfig() {
        return new EmailConfig(encryptValue, randomValue, UID_KEY, SynchronizationType.NOTHING);
    }
}
