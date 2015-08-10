package com.yazino.email;

import java.util.Map;

public interface EmailService {

    String PROPERTY_EMAIL_ENABLED = "email.enabled";

    void send(String toAddress,
              String fromAddress,
              String subject,
              String templateName,
              Map<String, Object> templateProperties)
            throws EmailException;

    void send(String[] toAddress,
              String fromAddress,
              String subject,
              String templateName,
              Map<String, Object> templateProperties)
            throws EmailException;

}
