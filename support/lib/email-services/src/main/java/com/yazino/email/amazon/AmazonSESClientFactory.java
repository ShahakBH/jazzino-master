package com.yazino.email.amazon;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;

import static org.apache.commons.lang3.Validate.notNull;

public class AmazonSESClientFactory {

    AmazonSimpleEmailServiceClient clientFor(final String accessKey,
                                             final String secretKey,
                                             final AmazonSESConfiguration amazonSESConfiguration) {
        notNull(accessKey, "accessKey may not be null");
        notNull(secretKey, "secretKey may not be null");
        notNull(amazonSESConfiguration, "amazonSESConfiguration may not be null");

        return new AmazonSimpleEmailServiceClient(
                new BasicAWSCredentials(accessKey, secretKey), amazonSESConfiguration.asConfiguration());
    }

}
