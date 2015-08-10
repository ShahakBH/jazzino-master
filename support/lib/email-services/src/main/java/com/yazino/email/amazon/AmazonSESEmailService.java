package com.yazino.email.amazon;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.*;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailException;
import com.yazino.email.EmailService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.DisplayTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

public class AmazonSESEmailService implements EmailService {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonSESEmailService.class);

    private static final String LOGGER_CLASS = "com.yazino.velocity.VelocityCommonsLogChute";

    private String templateDirectory;
    private String templatePrefix;
    private VelocityEngine velocityEngine;

    private final AmazonSimpleEmailServiceClient client;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public AmazonSESEmailService(@Value("${strata.aws.ses.access-key}") final String accessKey,
                                 @Value("${strata.aws.ses.secret-key}") final String secretKey,
                                 final AmazonSESClientFactory clientFactory,
                                 final AmazonSESConfiguration amazonSESConfiguration,
                                 final YazinoConfiguration yazinoConfiguration) {
        notNull(accessKey, "accessKey may not be null");
        notNull(secretKey, "secretKey may not be null");
        notNull(clientFactory, "clientFactory may not be null");
        notNull(amazonSESConfiguration, "amazonSESConfiguration may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        client = clientFactory.clientFor(accessKey, secretKey, amazonSESConfiguration);

        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public void send(final String toAddress,
                     final String fromAddress,
                     final String subject,
                     final String templateName,
                     final Map<String, Object> templateProperties)
            throws EmailException {
        if (StringUtils.isBlank(toAddress)) {
            throw new EmailException("toAddress may not be null/empty");
        }

        send(new String[]{toAddress}, fromAddress, subject, templateName, templateProperties);
    }

    @Override
    public void send(final String[] toAddresses,
                     final String fromAddress,
                     final String subject,
                     final String templateName,
                     final Map<String, Object> templateProperties)
            throws EmailException {
        if (StringUtils.isBlank(fromAddress)) {
            throw new EmailException("fromAddress may not be null/empty");
        }
        notEmpty(toAddresses, "toAddresses may not be null/empty");
        notNull(templateName, "templateName may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Send to:[{}] subject:[{}] template:[{}] properties:[{}]",
                    ArrayUtils.toString(toAddresses), subject, templateName, templateProperties);
        }

        if (!yazinoConfiguration.getBoolean(PROPERTY_EMAIL_ENABLED, true)) {
            LOG.warn("Email is disabled, email will not be sent");
            return;
        }

        final Set<String> invalidAddresses = getInvalidEmailAddressesFrom(toAddresses);
        if (!invalidAddresses.isEmpty()) {
            throw new EmailException("Invalid email addresses submitted: " + StringUtils.join(invalidAddresses, ","));
        }

        final String content = renderTemplate(templateName, templateProperties);

        try {
            final Message message = new Message(contentOf(subject),
                    new Body().withHtml(contentOf(content).withCharset("UTF-8")));
            final SendEmailRequest request = new SendEmailRequest(fromAddress, to(toAddresses), message);

            final SendEmailResult result = client.sendEmail(request);

            if (LOG.isInfoEnabled()) {
                LOG.info("Send completed to:[{}] subject:[{}] template:[{}] properties:[{}] msgId: [{}]",
                        ArrayUtils.toString(toAddresses), subject, templateName, templateProperties,
                        result.getMessageId());
            }

        } catch (Exception e) {
            if (e instanceof MessageRejectedException
                    && e.getMessage().startsWith("Address blacklisted")) {
                LOG.info("Addresses are blacklisted: {}", Arrays.toString(toAddresses));
            } else {
                LOG.error("Could not send email to {}", Arrays.toString(toAddresses), e);
                throw new EmailException("Could not send email to " + Arrays.toString(toAddresses), e);
            }
        }
    }

    private Set<String> getInvalidEmailAddressesFrom(final String[] addressList) {
        final Set<String> invalidAddresses = new HashSet<String>();
        for (String address : addressList) {
            final int separatorIndex = address.indexOf('@');
            if (separatorIndex < 0 || separatorIndex == (address.length() - 1)) {
                invalidAddresses.add(address);
            }
        }
        return invalidAddresses;
    }

    private Content contentOf(final String content) {
        if (content != null) {
            return new Content(content);
        }
        return new Content();
    }

    private Destination to(final String[] toAddresses) {
        return new Destination(asList(toAddresses));
    }

    private String renderTemplate(final String templateName,
                                  final Map<String, Object> properties)
            throws EmailException {
        initVelocity();

        final StringWriter templateWriter = new StringWriter();
        final VelocityContext velocityContext;
        if (properties != null) {
            velocityContext = new VelocityContext(properties);
        } else {
            velocityContext = new VelocityContext();
        }

        velocityContext.put("numberTool", new NumberTool());
        velocityContext.put("esc", new EscapeTool());
        velocityContext.put("display", new DisplayTool());

        String templateNameToLoad = templateName;
        if (templatePrefix != null) {
            templateNameToLoad = templatePrefix + templateNameToLoad;
        }
        if (!templateNameToLoad.toLowerCase().endsWith(".vm")) {
            templateNameToLoad += ".vm";
        }

        try {
            velocityEngine.mergeTemplate(templateNameToLoad, velocityContext, templateWriter);

        } catch (Exception e) {
            LOG.error("Template rendering failed for template {}", templateNameToLoad, e);
            throw new EmailException("Template rendering failed template " + templateNameToLoad, e);
        }

        return templateWriter.toString();
    }

    private void initVelocity() throws EmailException {
        if (velocityEngine != null) {
            return;
        }

        try {
            initVelocityEngine();

        } catch (Exception e) {
            LOG.error("Could not initialise velocity engine", e);
            throw new EmailException("Template engine not initialised", e);
        }
    }

    private void initVelocityEngine() throws Exception {
        velocityEngine = new VelocityEngine();

        if (templateDirectory != null) {
            LOG.debug("Template will be loaded from file system location: {}", templateDirectory);

            if (!new File(templateDirectory).exists()) {
                throw new IllegalStateException("Template directory does not exist: " + templateDirectory);
            }

            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
            velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
            velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templateDirectory);

        } else {
            LOG.debug("Template will be loaded from classpath");

            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
            velocityEngine.setProperty("class.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

            templatePrefix = "/template/";
        }

        velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, LOGGER_CLASS);
        velocityEngine.init();
    }

    public void setTemplateDirectory(final String templateDirectory) {
        if (StringUtils.isBlank(templateDirectory)) {
            this.templateDirectory = null;
        } else {
            this.templateDirectory = templateDirectory;
        }
    }
}
