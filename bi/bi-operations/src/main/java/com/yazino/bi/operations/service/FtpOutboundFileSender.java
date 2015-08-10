package com.yazino.bi.operations.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.io.File;

@Service("ftpOutboundFileSender")
public class FtpOutboundFileSender {

    private static final Logger LOG = LoggerFactory.getLogger(FtpOutboundFileSender.class);

    private MessageChannel messageChannel;

    @Autowired
    public FtpOutboundFileSender(@Qualifier("messageChannel") final MessageChannel messageChannel) {
        this.messageChannel = messageChannel;
    }

    public void transfer(final File file) {
        final Message<File> message = MessageBuilder.withPayload(file).build();
        if (LOG.isDebugEnabled()) {
            LOG.debug(file.length() + " bytes file '" + file + "' being transferred");
        }
        messageChannel.send(message);
    }

}
