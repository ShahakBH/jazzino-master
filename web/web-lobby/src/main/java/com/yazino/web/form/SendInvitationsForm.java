package com.yazino.web.form;

import com.yazino.validation.EmailAddressFormatValidator;
import org.springframework.validation.ObjectError;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class SendInvitationsForm {

    private String sentTo;
    private String message;
    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getSentTo() {
        return sentTo;
    }

    public void setSentTo(final String sentTo) {
        this.sentTo = sentTo;
    }

    public Set<String> getSendToList() {
        if (isBlank(sentTo)) {
            return new HashSet<String>();
        }
        final String cleanSendTo = this.sentTo.replace(";", ",").replace(" ", "");
        return new HashSet<String>(Arrays.asList(cleanSendTo.split(",")));
    }

    public String[] getSendToAddressesAsArray()    {
        if (isBlank(sentTo)) {
            return new String[0];
        }
        return this.sentTo.replace(";", ",").replace(" ", "").split(",");
    }

    public List<ObjectError> validate(final String objectName) {
        final List<ObjectError> errors = new ArrayList<ObjectError>();
        final Set<String> toAddresses = getSendToList();
        if (toAddresses.size() == 0) {
            errors.add(new ObjectError(objectName, "No recipients are supplied"));
            return errors;
        }
        for (final String to : toAddresses) {
            if (!EmailAddressFormatValidator.isValidFormat(to)) {
                errors.add(new ObjectError(objectName, to + " is not a vaild email address"));
            }
        }
        return errors;
    }
}
