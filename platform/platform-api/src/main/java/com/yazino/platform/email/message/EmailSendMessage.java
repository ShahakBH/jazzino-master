package com.yazino.platform.email.message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailSendMessage implements EmailMessage {
    private static final long serialVersionUID = 3112641501432495003L;

    private static final int VERSION = 1;

    private final Collection<String> recipients = new ArrayList<String>();
    private String sender;
    private final Map<String, Object> templateProperties = new HashMap<String, Object>();
    private String subject;
    private String templateName;

    public EmailSendMessage() {
    }

    public EmailSendMessage(final Collection<String> recipients,
                            final String sender,
                            final String subject,
                            final String templateName,
                            final Map<String, Object> templateProperties) {
        notEmpty(recipients, "recipients may not be empty");

        this.sender = sender;
        this.recipients.addAll(recipients);
        this.subject = subject;
        this.templateName = templateName;

        if (templateProperties != null) {
            this.templateProperties.putAll(templateProperties);
        }
    }

    public String getSender() {
        return sender;
    }

    public void setSender(final String sender) {
        this.sender = sender;
    }

    public int getVersion() {
        return VERSION;
    }

    @Override
    public Object getMessageType() {
        return "email";
    }

    public Collection<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(final Collection<String> recipients) {
        notEmpty(recipients, "recipients may not be empty");
        this.recipients.clear();
        this.recipients.addAll(recipients);
    }

    public Map<String, Object> getTemplateProperties() {
        return templateProperties;
    }

    public void setTemplateProperties(final Map<String, Object> templateProperties) {
        this.templateProperties.clear();
        if (templateProperties != null) {
            this.templateProperties.putAll(templateProperties);
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final EmailSendMessage rhs = (EmailSendMessage) obj;
        return new EqualsBuilder()
                .append(recipients, rhs.recipients)
                .append(subject, rhs.subject)
                .append(templateName, rhs.templateName)
                .append(templateProperties, rhs.templateProperties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(recipients)
                .append(subject)
                .append(templateName)
                .append(templateProperties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(recipients)
                .append(subject)
                .append(templateName)
                .append(templateProperties)
                .toString();
    }
}

