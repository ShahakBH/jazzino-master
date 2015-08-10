package com.yazino.bi.operations.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class FtpOutboundFileSenderIntegrationTest {

    @Autowired
    @Qualifier("messageChannel")
    private MessageChannel messageChannel;

    private FtpOutboundFileSender ftpOutboundFileSender;

    @Before
    public void init() {
        ftpOutboundFileSender = new FtpOutboundFileSender(messageChannel);
    }

    @Test
    public void testTransferFile() throws IOException {
        File file = new File("./bi-operations/src/main/resources/dummyFileToTestFTP.txt");
        ftpOutboundFileSender.transfer(file);
    }

}

