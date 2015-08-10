package com.yazino.email.templates;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailException;
import com.yazino.email.amazon.AmazonSESClientFactory;
import com.yazino.email.amazon.AmazonSESConfiguration;
import com.yazino.email.amazon.AmazonSESEmailService;

import java.io.File;
import java.util.*;

public class SendTestEmailHelper {
    public static final String TEMPLATE_GROUP = "lobby";

    private final String address = "contact@yazino.com";
    private final String accessKey = "AKIAJOSBBR6FVCQJUSVQ";
    private final String secretKey = "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";
    private final AmazonSESEmailService service;

    private Set<String> toAddresses = new HashSet<String>();
    private String template;
    private String subject = "test email";
    private Map<String, Object> properties = new HashMap<String, Object>();

    public SendTestEmailHelper(final Object projectPath) {
        final YazinoConfiguration yazinoConfiguration = new YazinoConfiguration();
        final AmazonSESConfiguration config = new AmazonSESConfiguration();
        config.setConnectionTimeout(10000);
        config.setSocketTimeout(5000);
        service = new AmazonSESEmailService(accessKey, secretKey, new AmazonSESClientFactory(), config, yazinoConfiguration);
        final String templateDir = String.format("%s/src/main/resources/%s", projectPath, TEMPLATE_GROUP);
        if (!new File(templateDir).exists()) {
            System.err.println("Template directory does not exist:" + templateDir);
            System.exit(-1);
        }
        service.setTemplateDirectory(templateDir);
    }

    public SendTestEmailHelper toAddress(String email) {
        toAddresses.add(email);
        return this;
    }

    public SendTestEmailHelper andAddress(String email){
        return toAddress(email);
    }

    public SendTestEmailHelper toAddresses(String firstEmail, String... otherEmails) {
        toAddresses.add(firstEmail);
        toAddresses.addAll(Arrays.asList(otherEmails));
        return this;
    }

    public SendTestEmailHelper withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public SendTestEmailHelper usingTemplate(String templateName) {
        this.template = templateName;
        return this;
    }

    public SendTestEmailHelper withProperty(String propertyName, Object propertValue) {
        properties.put(propertyName, propertValue);
        return this;
    }

    public void send() throws EmailException {
        service.send(toAddresses.toArray(new String[toAddresses.size()]), address, subject, template, properties);
    }

}
