package com.yazino.engagement.email.infrastructure;

import com.google.common.base.Charsets;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.EmailTarget;
import com.yazino.engagement.email.domain.EmailVisionResponse;
import com.yazino.engagement.email.domain.EmailVisionStatusResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class CampaignCommanderClient {

    private final String baseUrl;
    private final String username;
    private final String password;
    private final String apikey;
    private final RestOperations xmlDecodingRestOperations;
    private final YazinoConfiguration configuration;

    private RestTemplate restTemplate = new RestTemplate();
    private static final Logger LOG = LoggerFactory.getLogger(CampaignCommanderClient.class);

    //CGLIB
    CampaignCommanderClient() {
        this(null, null, null, null, null, null);
    }

    @Autowired
    public CampaignCommanderClient(
            @Value("${emailvision.campaign.baseurl}") String baseUrl,
            @Value("${emailvision.campaign.username}") String username,
            @Value("${emailvision.campaign.password}") String password,
            @Value("${emailvision.campaign.apikey}") String apikey,
            @Qualifier("restOperations") RestOperations xmlDecodingRestOperations,
            YazinoConfiguration configuration) {

        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.apikey = apikey;
        this.xmlDecodingRestOperations = xmlDecodingRestOperations;
        this.configuration = configuration;
    }

    public boolean deliverCampaign(final Long campaignRunId, final String templateId, final String filter120days) {


        String token;
        boolean isSuccess;
        try {
            token = openConnectionForCampaignManagement();
            String segmentName = format("segment for campaign %s", campaignRunId);
            Long segmentId = createSegment(token, segmentName, segmentName);
            addStringDemographicCriteriaToSegment(token, segmentId, "SOURCE", "EQUALS", campaignRunId.toString());

            String filter120daysString = getFilter120daysNullWrapper(filter120days);
            if (filter120daysString.equalsIgnoreCase("ON")) {
                addRecencyCriteriaToSegment(token, segmentId, "LAST_DATE_OPEN", "ISBETWEEN_RELATIVE", 0, 120);
            }

            String templateName = format("template of %s for campaignRunId %s", templateId, campaignRunId);
            final String cloneMessageId = cloneMessage(token, templateId, templateName);
            final String campaignId = createCampaign(token,
                    format("campaign for campaignRunId %s", campaignRunId),
                    new DateTime(),
                    cloneMessageId,
                    segmentId);
            isSuccess = postCampaign(token, campaignId);

        } catch (Exception e) {
            LOG.error("Did not send emails for campaignrunId {}", campaignRunId, e);
            return false;
        }

        closeCampaignConnection(token);

        if (isSuccess) {
            LOG.info("successfully sent email campaign for campaignRunId: {}", campaignRunId);
        } else {
            LOG.info("successfully set up email campaign for campaignRunId it still needs to be sent by "
                    + "Email Vision Campaign Commander if you want it sent : {}", campaignRunId);
        }
        return isSuccess;
    }

    private String getFilter120daysNullWrapper(final String filter120days) {
        if (filter120days == null) {
            return "";
        }

        return filter120days;
    }

    public Long addEmailAddresses(List<EmailTarget> emailTargets,
                                  final Long campaignRunId,
                                  final String campaignTitle) {
        StringBuilder csv = new StringBuilder();
        Set<String> contentColumns = newLinkedHashSet();
        for (EmailTarget emailTarget : emailTargets) {
            final Map<String, Object> content = filter(emailTarget.getContent());
            if (content != null) {
                contentColumns.addAll(content.keySet());
            }
        }
        for (EmailTarget emailTarget : emailTargets) {

            try {
                csv.append(format("%s\t%s\t%s%s\n",
                        getDefaultedDisplayName(emailTarget),
                        emailTarget.getEmailAddress().replaceAll("\t", ""),
                        campaignRunId,
                        contentFrom(emailTarget, contentColumns)));
            } catch (Exception e) {
                LOG.warn("couldn't add emailTarget to campaign run: " + emailTarget.toString(), e);
            }
        }

        final String contentDefinition = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n"
                + "<mergeUpload> \n"
                + "<fileName>%s</fileName> \n"
                + "<separator>tab</separator> \n"
                + "<criteria>EMAIL</criteria> \n"
                + "<skipFirstLine>false</skipFirstLine> \n"
                + "<fileEncoding>UTF-8</fileEncoding> \n"
                + "<autoMapping>false</autoMapping> \n"
                + "<mapping> \n"
                + "<column> \n"
                + "<colNum>1</colNum> \n"
                + "<fieldName>DISPLAY_NAME</fieldName> \n"
                + "<toReplace>true</toReplace> \n"
                + "</column> \n"
                + "<column> \n"
                + "<colNum>2</colNum> \n"
                + "<fieldName>EMAIL</fieldName> \n"
                + "<toReplace>false</toReplace> \n"
                + "</column> \n"
                + "<column> \n"
                + "<colNum>3</colNum> \n"
                + "<fieldName>SOURCE</fieldName> \n"
                + "<toReplace>true</toReplace> \n"
                + "</column> \n"
                + "%s"
                + "</mapping> \n"
                + "</mergeUpload>", campaignTitle, extraColumnDefinitions(contentColumns));


        HttpEntity request = buildMultipartHttpEntity(contentDefinition, csv);
        LOG.debug("HttpEntity is:");
        LOG.debug(request.getHeaders().toString());
        LOG.debug(request.getBody().toString());

        final String token = openConnectionForBulkDataManagement();

        final Long uploadId;
        try {
            String url = format("%s/apibatchmember/services/rest/batchmemberservice/%s/batchmember/mergeUpload", baseUrl, token);
            final EmailVisionResponse resultBody = getRestTemplate().exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    EmailVisionResponse.class).getBody();
            uploadId = Long.valueOf(resultBody.getResult());
            LOG.info("uploaded {} emails, result is ID ", emailTargets.size(), uploadId.toString());
            LOG.debug(url);
            LOG.debug(contentDefinition);
            LOG.debug(csv.toString());
            LOG.debug(resultBody.toString());
        } finally {
            closeConnection(token);
        }

        return uploadId;
    }

    private String getDefaultedDisplayName(final EmailTarget emailTarget) {
        if (isBlank(emailTarget.getDisplayName())) {
            return "Player";
        } else {
            return emailTarget.getDisplayName().replaceAll("\t", "");
        }
    }

    private Map<String, Object> filter(final Map<String, Object> content) {
        HashMap<String, Object> result = newHashMap();
        if (content != null) {
            for (String key : content.keySet()) {
                if (isNotInListAlready(key)) {
                    result.put(key, content.get(key));
                }
            }
        }
        return result;
    }

    private boolean isNotInListAlready(final String key) {
        final Set<String> columns = newHashSet("DISPLAY_NAME", "EMAIL", "SOURCE");
        return !columns.contains(key.toUpperCase());
    }

    private String extraColumnDefinitions(final Set<String> contentColumns) {
        final StringBuilder column = new StringBuilder();
        int columnCounter = 4;
        for (String contentColumn : contentColumns) {
            column
                    .append("<column> \n")
                    .append(format("<colNum>%s</colNum> \n", columnCounter++))
                    .append(format("<fieldName>%s</fieldName> \n", contentColumn))
                    .append("<toReplace>true</toReplace> \n")
                    .append("</column> \n");

        }
        return column.toString();
    }

    private String contentFrom(final EmailTarget emailTarget, final Set<String> contentColumns) {
        StringBuilder result = new StringBuilder("");
        final Map<String, Object> content = emailTarget.getContent();

        for (String column : contentColumns) {
            result.append("\t");
            if (content != null) {
                final Object o = content.get(column);
                if (o != null) {
                    final String value = stripTabsFromValue(o);
                    if (column.equals("BALANCE")) {
                        try {
                            result.append(new DecimalFormat("#,###").format(Double.parseDouble(value)));
                        } catch (Exception e) {
                            result.append(value);
                        }
                    } else {
                        result.append(value);
                    }
                }
            }
        }
        return result.toString();
    }

    private String stripTabsFromValue(final Object o) {
        return o.toString().replaceAll("\t", "");
    }

    private HttpEntity buildMultipartHttpEntity(final String contentDefinition, final StringBuilder csv) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        final HttpHeaders xmlHeaders = new HttpHeaders();
        xmlHeaders.add("Content-Type", "text/xml");
        body.add("mergeUpload", new HttpEntity<Resource>(new ByteArrayResource(contentDefinition.getBytes(Charsets.UTF_8)), xmlHeaders));

        final HttpHeaders csvHeaders = new HttpHeaders();
        csvHeaders.add("Content-Type", "application/octet-stream");
        csvHeaders.add("Content-Transfer-Encoding", "base64");
        byte[] byteArray = Base64.encodeBase64(csv.toString().getBytes(Charsets.UTF_8));
        LOG.debug("Base64 string : " + new String(byteArray));
        body.add("inputStream",
                new HttpEntity<Resource>(new ByteArrayResource(byteArray), csvHeaders));

        final HttpHeaders headers = new HttpHeaders();
        final Map<String, String> params = newHashMap();

        headers.setContentType(new MediaType("multipart", "form-data", params));


        return new HttpEntity<MultiValueMap>(body, headers);
    }

    protected RestTemplate getRestTemplate() {
        return restTemplate;
    }

    private String openConnectionForBulkDataManagement() {
        String url = format("%s/apibatchmember/services/rest/connect/open/%s/%s/%s",
                baseUrl,
                username,
                password,
                apikey);
        final EmailVisionResponse token = xmlDecodingRestOperations.getForObject(url, EmailVisionResponse.class);
        LOG.debug("opened connection result was {}", token);
        LOG.debug(url);

        checkResultForSuccessOrThrowRunTimeException(token, "could not open a connection to SMART FOCUS/ EMAIL VISION Bulk upload");

        return token.getResult();
    }

    public String openConnectionForCampaignManagement() {
        String url = format("%s/apiccmd/services/rest/connect/open/%s/%s/%s",
                baseUrl,
                configuration.getString("emailvision.campaign.segment.username"),
                configuration.getString("emailvision.campaign.segment.password"),
                configuration.getString("emailvision.campaign.segment.apikey"));

        final EmailVisionResponse token = xmlDecodingRestOperations.getForObject(url, EmailVisionResponse.class);
        LOG.debug("opened connection result was {}", token);
        LOG.debug(url);

        checkResultForSuccessOrThrowRunTimeException(token,
                "could not open a connection to SMART FOCUS/ EMAIL VISION campaign management api");

        return token.getResult();
    }

    public EmailVisionUploadStatus getUploadStatus(final long uploadId) {
        String token = openConnectionForBulkDataManagement();
        EmailVisionUploadStatus uploadStatus;
        String status = "";
        LOG.info("checking upload status for upload {}", uploadId);
        final String url = format("%s/apibatchmember/services/rest/batchmemberservice/%s/batchmember/%s/getUploadStatus",
                baseUrl,
                token,
                uploadId);
        try {
            final EmailVisionStatusResponse response = xmlDecodingRestOperations.getForObject(url, EmailVisionStatusResponse.class);
            status = response.getUploadStatus().getStatus();

            LOG.info("rec'd upload status for upload {}: {}", uploadId, status);

            uploadStatus = EmailVisionUploadStatus.getStatus(status);
            if (uploadStatus.isError() || uploadStatus == EmailVisionUploadStatus.DONE_WITH_ERRORS) {
                LOG.error("Errors uploading campaign with uploadId {}: {}", uploadId, response.getUploadStatus().getDetails());
            }
        } catch (IllegalArgumentException e) {
            LOG.error("failure parsing upload status of upload {}", uploadId);
            throw new IllegalArgumentException("failed to parse response of getUploadStatus all: " + status);
        } finally {
            closeConnection(token);
        }
        return uploadStatus;
    }

    private void closeConnection(final String token) {
        String url = format("%s/apibatchmember/services/rest/connect/close/%s",
                baseUrl,
                token);
        final EmailVisionResponse response = xmlDecodingRestOperations.getForObject(url, EmailVisionResponse.class);
        LOG.debug("closed connection for {} ", token);
        LOG.debug(url);
    }

    public void closeCampaignConnection(final String token) {
        String url = format("%s/apiccmd/services/rest/connect/close/%s",
                baseUrl,
                token);
        final EmailVisionResponse response = xmlDecodingRestOperations.getForObject(url, EmailVisionResponse.class);
        LOG.debug("closed connection for {} ", token);
        LOG.debug(url);

    }


    public Long createSegment(final String token, final String name, final String description) {
        Validate.notNull(token);
        Long segmentId = null;

        String xml = format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<segmentation>\n"
                + " <description>%s</description>\n"
                + " <name>%s</name>\n"
                + " <sampleType>ALL</sampleType>\n"
                + "</segmentation>",
                description, name);

        EmailVisionResponse resultBody = null;
        try {
            LOG.debug("Creating Segment");
            LOG.debug(xml);

            String url = format("%s/apiccmd/services/rest/segmentationservice/%s/segment", baseUrl, token);
            resultBody = getRestTemplate().exchange(
                    url,
                    HttpMethod.PUT,
                    getHttpEntity(xml),
                    EmailVisionResponse.class).getBody();
            segmentId = Long.valueOf(resultBody.getResult());
            LOG.info("created segment with Id of {}", segmentId);
            LOG.debug(url);
        } catch (Exception e) {
            LOG.error("could not create segment", e);
            throw new RuntimeException("could not create segment");
        }

        checkResultForSuccessOrThrowRunTimeException(resultBody, "could not create segment");
        return segmentId;
    }

    private void checkResultForSuccessOrThrowRunTimeException(final EmailVisionResponse resultBody, final String msg) {
        if (!resultBody.isSuccess()) {
            LOG.warn(msg);
            throw new RuntimeException(msg);
        }
    }

    public boolean addStringDemographicCriteriaToSegment(final String token,
                                                         final Long segmentId,
                                                         final String columnName,
                                                         final String operator,
                                                         final String value) {
        notNull(token);
        boolean result = false;

        String url = format("%s/apiccmd/services/rest/segmentationservice/%s/segment/%s/criteria/addStringDemographic",
                baseUrl,
                token,
                segmentId);
        String xml = format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<stringDemographicCriteria>\n"
                + " <columnName>%s</columnName>\n"
                + " <operator>%s</operator>\n"
                + " <values>%s</values>\n"
                + "</stringDemographicCriteria>", columnName, operator, value);

        EmailVisionResponse resultBody = null;
        try {
            LOG.debug("Adding String Demographic Criteria");
            LOG.debug(url);
            LOG.debug(xml);

            resultBody = getRestTemplate().exchange(
                    url,
                    HttpMethod.PUT,
                    getHttpEntity(xml),
                    EmailVisionResponse.class).getBody();
            result = Boolean.valueOf(resultBody.getResult());
            LOG.debug("adding string demographic criteria to segment {}");
        } catch (Exception e) {
            LOG.error("could not add source criteria to segment", e);
            throw new RuntimeException("could not add source criteria to segment");
        }

        checkResultForSuccessOrThrowRunTimeException(resultBody, "could not add source criteria to segment");
        return result;
    }


    public boolean addRecencyCriteriaToSegment(final String token,
                                               final Long segmentId,
                                               final String columnName,
                                               final String operator,
                                               final int periodStart,
                                               final int periodEnd) {
        boolean result = false;
        notNull(token);

        String url = format("%s/apiccmd/services/rest/segmentationservice/%s/segment/%s/criteria/addRecency", baseUrl, token, segmentId);
        String xml = format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<recencyCriteria>\n"
                + " <columnName>%s</columnName>\n"
                + " <periodDayBeginList>%s</periodDayBeginList>\n"
                + " <periodDayEndList>%s</periodDayEndList>\n"
                + " <operator>%s</operator>\n"
                + "</recencyCriteria>", columnName, periodStart, periodEnd, operator);

        EmailVisionResponse resultBody;
        try {
            LOG.debug("Adding Recency Criteria");
            LOG.debug(url);
            LOG.debug(xml);

            resultBody = getRestTemplate().exchange(
                    url,
                    HttpMethod.PUT,
                    getHttpEntity(xml),
                    EmailVisionResponse.class).getBody();
            result = Boolean.valueOf(resultBody.getResult());
            LOG.debug("adding string demographic criteria to segment {}");
        } catch (Exception e) {
            LOG.error("could not add recency criteria to segment", e);
            throw new RuntimeException("could not add recency criteria to segment");

        }

        checkResultForSuccessOrThrowRunTimeException(resultBody, "could not add recency criteria to segment");
        return result;
    }

    // id of message to clone
    public String cloneMessage(final String token, final String id, final String name) {
        notNull(token);
        EmailVisionResponse response = null;
        String url = format("%s/apiccmd/services/rest/message/cloneMessage/%s/%s/%s",
                baseUrl,
                token,
                id,
                name);

        try {
            LOG.debug("cloning message: {}", url);
            response = xmlDecodingRestOperations.getForObject(url, EmailVisionResponse.class);
            LOG.debug(url);
        } catch (Exception e) {
            LOG.error("could not create segment", e);
            throw new RuntimeException("could not clone message id = {}" + id);
        }

        checkResultForSuccessOrThrowRunTimeException(response, "could not clone message with id=" + id);
        LOG.debug("cloned message id: {}", response.getResult());

        return response.getResult();
    }

    public String createCampaign(final String token,
                                 final String name,
                                 final DateTime sendDate,
                                 final String messageId,
                                 final Long segmentId) {
        notNull(token);
        Boolean notifyProgress = configuration.getBoolean("emailvision.campaign.notifProgress", Boolean.TRUE);
        Boolean postClickTracking = configuration.getBoolean("emailvision.campaign.postClickTracking", Boolean.TRUE);
        Boolean emailDeduplicate = configuration.getBoolean("emailvision.campaign.emaildedupflg", Boolean.TRUE);

        final String url = format("%s/apiccmd/services/rest/campaign/create/%s/%s/%s/%s/%s/%s/%s/%s/%s",
                baseUrl,
                token,
                name,
                name,
                sendDate,
                messageId,
                segmentId,
                notifyProgress.toString(),
                postClickTracking.toString(),
                emailDeduplicate.toString());
        EmailVisionResponse response = null;
        try {
            LOG.debug("creating campaign");
            LOG.debug(url);
            response = xmlDecodingRestOperations.getForObject(url, EmailVisionResponse.class);
            LOG.info("campaign creation result {} {}", response.getResponseStatus(), response.getResult());

            if (!response.getResponseStatus().equals("success")) {
                LOG.error("failed to create campaign {}", response);
            }
        } catch (Exception e) {
            LOG.error("could not create segment", e);
            throw new RuntimeException("could not create segment");
        }

        checkResultForSuccessOrThrowRunTimeException(response, "could not create segment");
        return response.getResult();
    }

    public boolean postCampaign(final String token, final String campaignId) {
        notNull(token);

        Boolean isPostCampaign = configuration.getBoolean("emailvision.campaign.postcampaign", Boolean.FALSE);

        EmailVisionResponse result = null;
        String url = format("%s/apiccmd/services/rest/campaign/post/%s/%s",
                baseUrl,
                token,
                campaignId);

        try {
            if (isPostCampaign) {
                LOG.debug("posting campaign");
                LOG.debug(url);
                result = xmlDecodingRestOperations.getForObject(url, EmailVisionResponse.class);
                LOG.debug("post campaign result was {}", result);
            } else {
                LOG.warn("campaign auto posting is disabled set emailvision.campaign.postcampaign to enable");
                return false;
            }
        } catch (Exception e) {
            LOG.error("could not post campaign", e);
            throw new RuntimeException("could not post campaign");
        }

        return result.isSuccess();
    }


    private HttpEntity getHttpEntity(final String xml) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        return new HttpEntity<>(xml, headers);
    }
}
