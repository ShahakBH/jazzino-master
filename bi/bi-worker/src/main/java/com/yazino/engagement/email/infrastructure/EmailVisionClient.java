package com.yazino.engagement.email.infrastructure;

import com.yazino.engagement.email.domain.EmailConfig;
import com.yazino.engagement.email.domain.EmailData;
import com.yazino.engagement.email.domain.EmailVisionZeroDayResponse;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.yazino.engagement.email.domain.EmailVisionRestParams.*;
import static org.apache.commons.collections.MapUtils.isNotEmpty;

@Service
public class EmailVisionClient implements EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(EmailVisionClient.class);

    protected static final String URL_PARAMS = "?random={random}&encrypt={encrypt}"
            + "&email={email}&senddate={senddate}&uidkey={uidkey}&stype={stype}";

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private final RestOperations restOperations;

    private final String baseUrl;

    @Autowired
    public EmailVisionClient(RestOperations restOperations,
                             @Value("${emailvision.dayzero.baseurl}") String baseUrl) {
        this.restOperations = restOperations;
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean sendEmail(final EmailConfig emailConfig, final EmailData emailData) {
        String url = baseUrl + URL_PARAMS + createDynamicKeysUrl(emailData.getDynamicKeys());
        Map<String, ?> uriVariables = createUriVariables(emailConfig, emailData);

        try {
            EmailVisionZeroDayResponse response = restOperations.getForObject(url, EmailVisionZeroDayResponse.class,
                    uriVariables);
            if (response != null && response.isSuccess()) {
                LOG.debug("Sending email succeeded for emailAddress: {}", emailData.getEmailAddress());
                return true;
            } else {
                LOG.error("Sending email failed with value {} for emailConfig {} with emailData {}", response,
                        emailConfig, emailData);
                return false;
            }
        } catch (RestClientException e) {
            LOG.info("Sending email failed with value for emailConfig {} with emailData {}", emailConfig, emailData, e);
            return false;
        }
    }

    protected String createDynamicKeysUrl(Map<String, String> dynamicKeysMap) {
        StringBuilder stringBuilder = new StringBuilder();
        if (isNotEmpty(dynamicKeysMap)) {
            stringBuilder.append("&dyn=");
            for (String key : dynamicKeysMap.keySet()) {
                stringBuilder.append(key).append(":");
                stringBuilder.append(dynamicKeysMap.get(key));
                stringBuilder.append("|");
            }
        }
        String dynamicKeysUrl = stringBuilder.toString();
        if (dynamicKeysUrl.length() > 0) {
            dynamicKeysUrl = dynamicKeysUrl.substring(0, dynamicKeysUrl.length() - 1);
        }
        return dynamicKeysUrl;
    }

    protected Map<String, ?> createUriVariables(EmailConfig emailConfig, EmailData emailData) {
        Map<String, String> uriVariables = new LinkedHashMap<String, String>();
        uriVariables.put(random.toString(), emailConfig.getRandomValue());
        uriVariables.put(encrypt.toString(), emailConfig.getEncryptValue());
        uriVariables.put(email.toString(), emailData.getEmailAddress());
        uriVariables.put(senddate.toString(), dateTimeFormatter.print(emailData.getSendDate()));
        uriVariables.put(uidkey.toString(), emailConfig.getUidKey());
        uriVariables.put(stype.toString(), emailConfig.getSynchronizationType().toString());
        return uriVariables;
    }

}
