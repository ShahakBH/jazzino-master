package com.yazino.engagement.email.domain;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType
@XmlRootElement(name = "response")
public class EmailVisionResponse {

    private String responseStatus;
    private String result;

    public EmailVisionResponse() {
    }

    public EmailVisionResponse(final String responseStatus, final String result) {
        this.responseStatus = responseStatus;
        this.result = result;
    }

    @XmlAttribute
    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(responseStatus);
    }

    @Override
    public String toString() {
        return String.format("response: %s, result: %s", responseStatus, result);
    }
}
