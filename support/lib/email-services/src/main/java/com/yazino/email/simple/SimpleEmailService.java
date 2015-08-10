package com.yazino.email.simple;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailException;
import com.yazino.email.EmailService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.NumberTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This is a simple implementation of the email service that sends synchronously.
 */
public class SimpleEmailService implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleEmailService.class);

    private static final String LOGGER_CLASS = "com.yazino.velocity.VelocityCommonsLogChute";

    private final JavaMailSender mailSender;
    private final YazinoConfiguration yazinoConfiguration;

    private String templateDirectory;
    private String templatePrefix;
    private VelocityEngine velocityEngine;

    @Autowired
    public SimpleEmailService(@Qualifier("mailSender") final JavaMailSender mailSender,
                              final YazinoConfiguration yazinoConfiguration) {
        notNull(mailSender, "mailSender may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.mailSender = mailSender;
        this.yazinoConfiguration = yazinoConfiguration;

        if (mailSender instanceof JavaMailSenderImpl) {
            final JavaMailSenderImpl jms = (JavaMailSenderImpl) mailSender;
            LOG.info("Email will be sent via {}:{} ({}:{}) [{}]", jms.getHost(), jms.getPort(), jms.getUsername(),
                    jms.getPassword(), jms.getJavaMailProperties());
        } else {
            LOG.info("Email will be sent via {}", ToStringBuilder.reflectionToString(mailSender));
        }
    }


    @Override
    public void send(final String toAddress,
                     final String fromAddress,
                     final String subject,
                     final String templateName, // the template is expected to be in a resources/template directory
                     final Map<String, Object> templateProperties)
            throws EmailException {
        send(new String[]{toAddress}, fromAddress, subject, templateName, templateProperties);
    }

    @Override
    public void send(final String[] toAddress,
                     final String fromAddress,
                     final String subject,
                     final String templateName,
                     final Map<String, Object> templateProperties)
            throws EmailException {
        notNull(toAddress, "To Address may not be null");
        notNull(templateName, "Template Name may not be null");

        LOG.info("send toAddress:[{}] from:[{}] subject:[{}] templateName:[{}] templateProperties:[{}]",
                toAddress, fromAddress, subject, templateName, templateProperties);

        if (mailSender == null) {
            throw new IllegalStateException("Mail Sender has not be initialised");
        }

        if (!yazinoConfiguration.getBoolean(PROPERTY_EMAIL_ENABLED, true)) {
            LOG.warn("Email is disabled, email will not be sent");
            return;
        }

        final String content = renderTemplate(templateName, templateProperties);

        final MimeMessage message = mailSender.createMimeMessage();
        final MimeMessageHelper helper = new MimeMessageHelper(message);

        try {
            helper.setFrom(fromAddress);

            if (subject != null) {
                helper.setSubject(subject);
            }

            helper.setTo(toAddress);
            helper.setText(content, true);

            mailSender.send(message);

            LOG.info("send completed toAddress:[{}] subject:[{}] templateName:[{}] templateProperties:[{}]",
                    toAddress, subject, templateName, templateProperties);

        } catch (Exception e) {
            LOG.error("Could not send email to " + Arrays.toString(toAddress), e);
            throw new EmailException("Could not send email to " + Arrays.toString(toAddress), e);
        }
    }

    public void setTemplateDirectory(final String templateDirectory) {
        if (StringUtils.isBlank(templateDirectory)) {
            this.templateDirectory = null;
        } else {
            this.templateDirectory = templateDirectory;
        }
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

}
