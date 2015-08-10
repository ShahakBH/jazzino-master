package com.yazino.engagement.email.infrastructure;

import com.yazino.engagement.email.domain.EmailConfig;
import com.yazino.engagement.email.domain.EmailData;

public interface EmailSender {

    boolean sendEmail(EmailConfig emailConfig, EmailData emailData);
}
