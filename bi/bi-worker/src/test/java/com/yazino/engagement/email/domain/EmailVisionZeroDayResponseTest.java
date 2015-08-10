package com.yazino.engagement.email.domain;

import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EmailVisionZeroDayResponseTest {

    private final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();

    private final String emailAddress = "abc@example.com";
    private final String responseStatus = "success";
    private final String result = "SendRequest has been successfully saved!";
    private final String description = "Bad authentication!!! Random parameter is wrong  F02F2F623F0100F0.";
    private final String fields = " F02F2F623F0100F0";
    private final String status = "CREATE_SENDREQUEST_FAILED";

    @Before
    public void setUp() throws Exception {
        marshaller.setClassesToBeBound(EmailVisionZeroDayResponse.class);
    }

    @Test
    public void jaxbMarshallerShouldBeAbleToUnMarshalEmailVisionZeroDaySuccessfulResponse() {

        String xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>"
                + "<response responseStatus=\"" + responseStatus
                + "\" email=\"" + emailAddress + "\"><result xsi:type=\"xs:string\" "
                + "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + result + "</result></response>";
        Source source = new StreamSource(new StringReader(xmlResponse));
        EmailVisionZeroDayResponse response = (EmailVisionZeroDayResponse) marshaller.unmarshal(source);

        assertThat(response.getEmailAddress(), is(equalTo(emailAddress)));
        assertThat(response.getResponseStatus(), is(equalTo(responseStatus)));
        assertThat(response.getResult(), is(equalTo(result)));
    }

    @Test
    public void jaxbMarshallerShouldBeAbleToUnMarshalEmailVisionZeroDayUnSuccessfulResponse() {

        String xmlResponse = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>"
                + "<response responseStatus=\"failed\">"
                + "<description>" + description + "</description>"
                + "<fields>" + fields + "</fields>"
                + "<status>" + status + "</status></response>";

        Source source = new StreamSource(new StringReader(xmlResponse));
        EmailVisionZeroDayResponse response = (EmailVisionZeroDayResponse) marshaller.unmarshal(source);

        assertThat(response.getResponseStatus(), is(equalTo("failed")));
        assertThat(response.getDescription(), is(equalTo(description)));
        assertThat(response.getFields(), is(equalTo(fields)));
        assertThat(response.getStatus(), is(equalTo(status)));
    }

    @Test
    // this test is here only for satisfying sonar metrics
    public void settersShouldUpdateFields() {
        EmailVisionZeroDayResponse response = new EmailVisionZeroDayResponse();
        response.setEmailAddress(emailAddress);
        response.setResponseStatus(responseStatus);
        response.setResult(result);
        response.setDescription(description);
        response.setFields(fields);
        response.setStatus(status);

        assertThat(response.getResult(), is(result));
        assertThat(response.getResponseStatus(), is(responseStatus));
        assertThat(response.getEmailAddress(), is(emailAddress));
        assertThat(response.getDescription(), is(description));
        assertThat(response.getFields(), is(fields));
        assertThat(response.getStatus(), is(status));

    }

}
