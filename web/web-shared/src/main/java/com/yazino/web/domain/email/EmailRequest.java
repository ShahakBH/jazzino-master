package com.yazino.web.domain.email;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.*;

public class EmailRequest {

    private final Collection<String> addresses = new HashSet<String>();
    private final String template;
    private final String subject;
    private final Map<String, Object> properties;
    private final String fromAddress;

    /** email sender will default to configured email if no fromAddress email is given*/
    public EmailRequest(final String template,
                        final String subject,
                        final Map<String, Object> properties,
                        final String... addresses) {
        this.addresses.addAll(validValuesFrom(addresses));
        this.template = template;
        this.subject = subject;
        this.properties = properties;
        this.fromAddress = null;
    }

    public EmailRequest(final String template,
                        final String subject,
                        final String fromAddress,
                        final Map<String, Object> properties,
                        final String... addresses) {
        this.fromAddress = fromAddress;
        this.addresses.addAll(validValuesFrom(addresses));
        this.template = template;
        this.subject = subject;
        this.properties = properties;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    private Collection<? extends String> validValuesFrom(final String[] sourceAddresses) {
        if (sourceAddresses == null || sourceAddresses.length == 0) {
            return Collections.emptySet();
        }

        final Set<String> filteredAddresses = new HashSet<String>();
        for (String address : sourceAddresses) {
            if (address != null) {
                filteredAddresses.add(address);
            }
        }
        return filteredAddresses;
    }

    public Collection<String> getAddresses() {
        return addresses;
    }

    public String getTemplate() {
        return template;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, Object> getProperties() {
        return properties;
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
        final EmailRequest rhs = (EmailRequest) obj;
        return new EqualsBuilder()
                .append(addresses, rhs.addresses)
                .append(template, rhs.template)
                .append(subject, rhs.subject)
                .append(properties, rhs.properties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(addresses)
                .append(template)
                .append(subject)
                .append(properties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
