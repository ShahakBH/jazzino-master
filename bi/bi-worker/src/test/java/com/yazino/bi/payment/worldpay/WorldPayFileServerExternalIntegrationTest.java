package com.yazino.bi.payment.worldpay;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class WorldPayFileServerExternalIntegrationTest {
    private static final String TEST_FILENAME = "MA.PISCESSW.#D.XRATE.DIOT.TRANSMIT";

    private final Set<File> tempFiles = new HashSet<>();

    @Autowired
    private WorldPayFileServer underTest;

    @After
    public void cleanUpTempFiles() {
        for (File tempFile : tempFiles) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Test
    public void fetchingAnExistingFileFromTheRemoteFileServerShouldDownloadTheFileToTheDestinationAndReturnTrue() throws IOException {
        final File destFile = createTempFileName();

        // if this starts failing check TEST_FILENAME has not been deleted
        final boolean found = underTest.fetchTo(TEST_FILENAME, destFile.getAbsolutePath());

        assertThat(found, is(true));
        assertThat(destFile.exists(), is(true));
        final BufferedReader reader = new BufferedReader(new FileReader(destFile));
        assertThat(reader.readLine(), startsWith("02"));
        reader.close();
    }

    @Test
    public void fetchingANonExistentFileFromTheRemoteFileServerShouldReturnFalse() throws IOException {
        final File destFile = createTempFileName();

        final boolean found = underTest.fetchTo("aNonExistentFile", destFile.getAbsolutePath());

        assertThat(found, is(false));
        assertThat(destFile.exists(), is(false));
    }

    private File createTempFileName() throws IOException {
        final File destFile = File.createTempFile("worldPayFileServerExternalIntegrationTest", ".tmp");
        tempFiles.add(destFile);
        destFile.delete();
        return destFile;
    }

}
