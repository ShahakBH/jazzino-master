package com.yazino.engagement.email.domain;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EmailConfigTest {

    @Test
    public void gettersShouldReturnTheValueSetInConstructor() {
        String encryptValue = "abcd";
        String randomValue = "z1x2c3";
        String uidKey = "email";
        EmailConfig emailConfig = new EmailConfig(encryptValue, randomValue, uidKey, SynchronizationType.NOTHING);

        assertThat(emailConfig.getEncryptValue(), is(equalTo(encryptValue)));
        assertThat(emailConfig.getRandomValue(), is(equalTo(randomValue)));
        assertThat(emailConfig.getUidKey(), is(equalTo(uidKey)));
        assertThat(emailConfig.getSynchronizationType(), is(SynchronizationType.NOTHING));
    }
}
