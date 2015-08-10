package com.yazino.engagement.email.domain;

import javax.xml.bind.annotation.*;

@XmlAccessorType
@XmlRootElement(name = "response")
public class EmailVisionStatusResponse {

    private String responseStatus;
    private UploadStatus uploadStatus;
    private String status;

    @XmlElement
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @XmlAttribute
    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }


    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(UploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(responseStatus);
    }

    @Override
    public String toString() {
        return String.format("response: %s, uploadstatus: %s, status: %s", responseStatus, uploadStatus, status);
    }

    @XmlType
    public static class UploadStatus {
        private String details;
        private String status;

        public UploadStatus() {
        }

        @XmlElement
        public String getDetails() {
            return details;
        }

        public void setDetails(final String details) {
            this.details = details;
        }

        @XmlElement
        public String getStatus() {
            return status;
        }

        public void setStatus(final String status) {
            this.status = status;
        }
    }
}
