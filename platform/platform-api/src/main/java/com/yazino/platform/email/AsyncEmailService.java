package com.yazino.platform.email;

import java.util.Collection;
import java.util.Map;

/**
 * Submit email to be sent in the background.
 */
public interface AsyncEmailService {

    /**
     * Send an email to a single recipient in the background.
     *
     * @param recipient          the address to send to. May not be null or blank.
     * @param sender             the sender. May not be null or empty.
     * @param subject            the subject. May be null.
     * @param templateName       the template name. May not be null.
     * @param templateProperties the properties for the template. May be null.
     */
    void send(String recipient,
              String sender,
              String subject,
              String templateName,
              Map<String, Object> templateProperties);

    /**
     * Send an email in the background.
     *
     * @param recipients         the addresses to send to. May not be null or empty.
     * @param sender             the sender. May not be null or empty.
     * @param subject            the subject. May be null.
     * @param templateName       the template name. May not be null.
     * @param templateProperties the properties for the template. May be null.
     */
    void send(Collection<String> recipients,
              String sender,
              String subject,
              String templateName,
              Map<String, Object> templateProperties);

    /**
     * Verify an address in the background.
     * <p/>
     * This will make the verification result available to future send requests.
     *
     * @param emailAddress the email address to verify. Not null.
     */
    void verifyAddress(String emailAddress);

}
