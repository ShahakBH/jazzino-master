package com.yazino.engagement.email.application;

import com.yazino.engagement.email.domain.EmailData;

public interface EmailApi {

    boolean sendDayZeroEmail(EmailData emailData);

}
