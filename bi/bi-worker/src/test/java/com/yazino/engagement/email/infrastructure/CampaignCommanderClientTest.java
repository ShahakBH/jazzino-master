package com.yazino.engagement.email.infrastructure;

import com.google.common.base.Charsets;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.EmailTarget;
import com.yazino.engagement.email.domain.EmailVisionResponse;
import com.yazino.engagement.email.domain.EmailVisionStatusResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.email.infrastructure.EmailVisionUploadStatus.DONE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CampaignCommanderClientTest {

    private static final String CAMPAIGN_TITLE = "campaign-2013-09-26";
    private static final String TOKEN = "TOKEN";

    @Mock
    private RestTemplate mockTemplate;
    @Mock
    private RestOperations restOps;
    private CampaignCommanderClient underTest;
    @Mock
    private EmailVisionResponse emailVisionResponse;
    private static final Long CAMPAIGN_RUN_ID = 123L;

    YazinoConfiguration configuration = new YazinoConfiguration();

    @Before
    public void setUp() throws Exception {
        configuration.addProperty("emailvision.campaign.segment.username", "username");
        configuration.addProperty("emailvision.campaign.segment.password", "password");
        configuration.addProperty("emailvision.campaign.segment.apikey", "apikey");

        underTest = new CampaignCommanderClient("baseUrl", "username", "password", "apikey", restOps, configuration) {
            @Override
            protected RestTemplate getRestTemplate() {
                return mockTemplate;
            }
        };
        when(emailVisionResponse.getResult()).thenReturn(TOKEN);
        final ResponseEntity responseEntity = mockResponse();

        when(restOps.getForObject(anyString(),
                argThat(new ClassOrSubclassMatcher<>(EmailVisionResponse.class)))).thenReturn(
                emailVisionResponse);

        when(mockTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(responseEntity);


        when(emailVisionResponse.isSuccess()).thenReturn(true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addEmailAddressShouldCallEmailVisionWithTokenFromEmailVision() {
        final List<EmailTarget> emailTargets = newArrayList();

        mockGetStatusResponse("DONE");

        underTest.addEmailAddresses(emailTargets, CAMPAIGN_RUN_ID, "Campaign Title");
        verify(mockTemplate).exchange(eq("baseUrl/apibatchmember/services/rest/batchmemberservice/TOKEN/batchmember/mergeUpload"),
                eq(HttpMethod.PUT), Mockito.any(HttpEntity.class),
                Mockito.any(Class.class));
        verifyConnectionOpenAndClosed();

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidGetStatusShouldReturnFail(){
        mockGetStatusResponse("not real");
        underTest.getUploadStatus(123L);
    }

    @Test(expected = RuntimeException.class)
    public void openConnectionShouldThrowRunTimeExceptionIfNotSuccessful(){
        when(emailVisionResponse.isSuccess()).thenReturn(false);
        underTest.openConnectionForCampaignManagement();
    }

    @Test
    public void openConnectionToCampaignManagementShouldReturnToken(){
        when(emailVisionResponse.isSuccess()).thenReturn(true);
        assertThat(underTest.openConnectionForCampaignManagement(), is(equalTo(TOKEN)));
    }

    @Test(expected = RuntimeException.class)
    public void createSegmentShouldThrowExceptionIfNotSuccessful()  {
        final ResponseEntity responseEntity = mock(ResponseEntity.class);
        final EmailVisionResponse bodyEntity = mock(EmailVisionResponse.class);
        when(bodyEntity.getResult()).thenReturn("123456");
        when(bodyEntity.isSuccess()).thenReturn(false);
        when(responseEntity.getBody()).thenReturn(bodyEntity);

        when(mockTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(
                responseEntity);


        assertThat(underTest.createSegment(TOKEN, "blah", "description"), is(equalTo(12345L)));

    }

    @Test
    public void createSegmentShouldReturnSegmentId()  {
        final ResponseEntity responseEntity = mock(ResponseEntity.class);
        final EmailVisionResponse bodyEntity = mock(EmailVisionResponse.class);
        when(bodyEntity.getResult()).thenReturn("123456");
        when(bodyEntity.isSuccess()).thenReturn(true);
        when(responseEntity.getBody()).thenReturn(bodyEntity);

        when(mockTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(
                responseEntity);


        assertThat(underTest.createSegment(TOKEN, "blah", "description"), is(equalTo(123456L)));
    }


    private void mockGetStatusResponse(String reponse) {
        final EmailVisionStatusResponse emailVisionStatusResponse=mock(EmailVisionStatusResponse.class);
        final EmailVisionStatusResponse.UploadStatus status=mock(EmailVisionStatusResponse.UploadStatus.class);

        when(restOps.getForObject(anyString(), argThat(new ClassOrSubclassMatcher<EmailVisionStatusResponse>(EmailVisionStatusResponse.class)))).thenReturn(emailVisionStatusResponse);
        when(emailVisionStatusResponse.getUploadStatus()).thenReturn(status);
        when(status.getStatus()).thenReturn(reponse);
    }

    private void verifyConnectionOpenAndClosed() {
        verify(restOps).getForObject(eq("baseUrl/apibatchmember/services/rest/connect/open/username/password/apikey"), any(Class.class));
        verify(restOps).getForObject(eq("baseUrl/apibatchmember/services/rest/connect/close/TOKEN"), any(Class.class));
    }

    @Test
    public void getUploadStatusShouldCheckStatus() {

        mockGetStatusResponse("DONE");
        final EmailVisionUploadStatus uploadStatus = underTest.getUploadStatus(123L);

        verifyConnectionOpenAndClosed();
        assertThat(uploadStatus, equalTo(DONE));
    }



    @SuppressWarnings("unchecked")
    @Test
    public void addEmailAddressesShouldCallEVWithMultipartData() throws IOException {
        String emailString = "Player\tbob@123.com\t123\tFACEBOOK_CANVAS\n";

        final List<EmailTarget> emailTargets = newArrayList(
                new EmailTarget("bob@123.com", null,newContent("REG_PLT","FACEBOOK_CANVAS")),
                new EmailTarget(null, "jim",null));
        final ResponseEntity responseEntity = mockResponse();
        when(mockTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(
                responseEntity);

        final Long uploadId = underTest.addEmailAddresses(emailTargets, CAMPAIGN_RUN_ID, CAMPAIGN_TITLE);
        assertThat(uploadId, equalTo(456L));
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(mockTemplate).exchange(anyString(), eq(HttpMethod.PUT), captor.capture(), eq(EmailVisionResponse.class));
        final HttpEntity entity = captor.getValue();
        final MultiValueMap<String, Object> body = (MultiValueMap<String, Object>) entity.getBody();
        assertThat(body.keySet().size(), is(2));

        final HttpEntity<Resource> insertUpload = (HttpEntity<Resource>) body.get("mergeUpload").get(0);
        final HttpEntity<Resource> inputStream = (HttpEntity<Resource>) body.get("inputStream").get(0);
        checkDefinition(insertUpload);
        final String streamString = new String(Base64.decodeBase64(((ByteArrayResource) inputStream.getBody()).getByteArray()),
                Charsets.UTF_8);
        assertThat(emailString, equalTo(streamString));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void addEmailAddressesShouldNotBlowUpWithBrokenData() throws IOException {
        String emailString = "bob\tbob@123.com\t123\tFACEBOOK_CANVAS\n"
                + "jim\tjim@123.com\t123\t\n";

        final List<EmailTarget> emailTargets = newArrayList(
                new EmailTarget("bob@123.com", "bob",newContent("REG_PLT","FACEBOOK_CANVAS")),
                new EmailTarget("jim@123.com", "jim",null));
        final ResponseEntity responseEntity = mockResponse();
        when(mockTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(
                responseEntity);

        final Long uploadId = underTest.addEmailAddresses(emailTargets, CAMPAIGN_RUN_ID, CAMPAIGN_TITLE);
        assertThat(uploadId, equalTo(456L));
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(mockTemplate).exchange(anyString(), eq(HttpMethod.PUT), captor.capture(), eq(EmailVisionResponse.class));
        final HttpEntity entity = captor.getValue();
        final MultiValueMap<String, Object> body = (MultiValueMap<String, Object>) entity.getBody();
        assertThat(body.keySet().size(), is(2));

        final HttpEntity<Resource> insertUpload = (HttpEntity<Resource>) body.get("mergeUpload").get(0);
        final HttpEntity<Resource> inputStream = (HttpEntity<Resource>) body.get("inputStream").get(0);
        checkDefinition(insertUpload);
        final String streamString = new String(Base64.decodeBase64(((ByteArrayResource) inputStream.getBody()).getByteArray()),
                Charsets.UTF_8);
        assertThat(emailString, equalTo(streamString));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void balanceFieldShouldBeFormattedWithCommasAndTabsShouldBeStripped() throws IOException {
        String correctResult = "bob\tbob@123.com\t123\t123,456,789\n"
                + "jim jam\tjimtab@123.com\t123\t\n";

        final List<EmailTarget> emailTargets = newArrayList(
                new EmailTarget("bob@123.com", "bob",newContent("BALANCE","123456789.30")),
                new EmailTarget("jim\ttab@123.com", "jim\t jam",null));
        final ResponseEntity responseEntity = mockResponse();
        when(mockTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(
                responseEntity);

        final Long uploadId = underTest.addEmailAddresses(emailTargets, CAMPAIGN_RUN_ID, CAMPAIGN_TITLE);
        assertThat(uploadId, equalTo(456L));
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(mockTemplate).exchange(anyString(), eq(HttpMethod.PUT), captor.capture(), eq(EmailVisionResponse.class));
        final HttpEntity entity = captor.getValue();
        final MultiValueMap<String, Object> body = (MultiValueMap<String, Object>) entity.getBody();
        assertThat(body.keySet().size(), is(2));

        final HttpEntity<Resource> insertUpload = (HttpEntity<Resource>) body.get("mergeUpload").get(0);
        final HttpEntity<Resource> inputStream = (HttpEntity<Resource>) body.get("inputStream").get(0);
        checkDefinitionWithBalance(insertUpload);
        final String streamString = new String(Base64.decodeBase64(((ByteArrayResource) inputStream.getBody()).getByteArray()),
                Charsets.UTF_8);
        assertThat(correctResult, equalTo(streamString));
    }

    private void checkDefinitionWithBalance(final HttpEntity<Resource> insertUpload) throws IOException {
        String contentDefinitionShouldBe ="<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
                "<mergeUpload> \n" +
                "<fileName>campaign-2013-09-26</fileName> \n" +
                "<separator>tab</separator> \n" +
                "<criteria>EMAIL</criteria> \n" +
                "<skipFirstLine>false</skipFirstLine> \n" +
                "<fileEncoding>UTF-8</fileEncoding> \n" +
                "<autoMapping>false</autoMapping> \n" +
                "<mapping> \n" +
                "<column> \n" +
                "<colNum>1</colNum> \n" +
                "<fieldName>DISPLAY_NAME</fieldName> \n" +
                "<toReplace>true</toReplace> \n" +
                "</column> \n" +
                "<column> \n" +
                "<colNum>2</colNum> \n" +
                "<fieldName>EMAIL</fieldName> \n" +
                "<toReplace>false</toReplace> \n" +
                "</column> \n" +
                "<column> \n" +
                "<colNum>3</colNum> \n" +
                "<fieldName>SOURCE</fieldName> \n" +
                "<toReplace>true</toReplace> \n" +
                "</column> \n" +
                "<column> \n" +
                "<colNum>4</colNum> \n" +
                "<fieldName>BALANCE</fieldName> \n" +
                "<toReplace>true</toReplace> \n" +
                "</column> \n" +
                "</mapping> \n" +
                "</mergeUpload>";
        final String content = IOUtils.toString(insertUpload.getBody().getInputStream());
        assertThat(contentDefinitionShouldBe, equalTo(content));
    }

    private void checkDefinition(final HttpEntity<Resource> insertUpload) throws IOException {
        String contentDefinitionShouldBe ="<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
                "<mergeUpload> \n" +
                "<fileName>campaign-2013-09-26</fileName> \n" +
                "<separator>tab</separator> \n" +
                "<criteria>EMAIL</criteria> \n" +
                "<skipFirstLine>false</skipFirstLine> \n" +
                "<fileEncoding>UTF-8</fileEncoding> \n" +
                "<autoMapping>false</autoMapping> \n" +
                "<mapping> \n" +
                "<column> \n" +
                "<colNum>1</colNum> \n" +
                "<fieldName>DISPLAY_NAME</fieldName> \n" +
                "<toReplace>true</toReplace> \n" +
                "</column> \n" +
                "<column> \n" +
                "<colNum>2</colNum> \n" +
                "<fieldName>EMAIL</fieldName> \n" +
                "<toReplace>false</toReplace> \n" +
                "</column> \n" +
                "<column> \n" +
                "<colNum>3</colNum> \n" +
                "<fieldName>SOURCE</fieldName> \n" +
                "<toReplace>true</toReplace> \n" +
                "</column> \n" +
                "<column> \n" +
                "<colNum>4</colNum> \n" +
                "<fieldName>REG_PLT</fieldName> \n" +
                "<toReplace>true</toReplace> \n" +
                "</column> \n" +
                "</mapping> \n" +
                "</mergeUpload>";
        final String content = IOUtils.toString(insertUpload.getBody().getInputStream());
        assertThat(contentDefinitionShouldBe, equalTo(content));
    }

    private Map<String, Object> newContent(final String contentType, final String contentValue) {
        final HashMap<String,Object> content = newHashMap();
        content.put(contentType, contentValue);
        return content;
    }

    @Test
    public void Should(){
        underTest.deliverCampaign(CAMPAIGN_RUN_ID, "23123", null);
//     Assert.assertThat(, CoreMatchers.is(IsEqual.equalTo()));
    }

    private ResponseEntity mockResponse() {
        final ResponseEntity responseEntity = mock(ResponseEntity.class);
        final EmailVisionResponse bodyEntity = mock(EmailVisionResponse.class);
        when(bodyEntity.getResult()).thenReturn("456");
        when(bodyEntity.isSuccess()).thenReturn(true);
        when(responseEntity.getBody()).thenReturn(bodyEntity);
        return responseEntity;
    }

    public class ClassOrSubclassMatcher<T> extends BaseMatcher<Class<T>> {

        private final Class<T> targetClass;

        public ClassOrSubclassMatcher(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        @SuppressWarnings("unchecked")
        public boolean matches(Object obj) {
            if (obj != null) {
                if (obj instanceof Class) {
                    return targetClass.isAssignableFrom((Class<T>) obj);
                }
            }
            return false;
        }

        public void describeTo(Description desc) {
            desc.appendText("Matches a class or subclass");
        }
    }
}
