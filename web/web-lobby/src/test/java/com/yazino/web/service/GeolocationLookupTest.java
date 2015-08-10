package com.yazino.web.service;

import com.maxmind.geoip.LookupService;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GeolocationLookupTest {
    private static final String US_IP_ADDRESS = "218.189.13.15";
    private static final String UK_IP_ADDRESS = "88.211.55.18";
    private static final String NORWAY_IP_ADDRESS = "213.52.50.8";
    private static final String FRANCE_IP_ADDRESS = "80.15.191.171";
    private static final String HAMMERSMITH_IP_ADDRESS = "88.211.55.18";
    private static final String MADE_UP = "10.9.8.8";

    @Test
    public void shouldFindNorwayByIP() throws Exception {
        String unitedStates = new GeolocationLookup().lookupCountryCodeByIp(US_IP_ADDRESS);
        String uk = new GeolocationLookup().lookupCountryCodeByIp(UK_IP_ADDRESS);
        String norway = new GeolocationLookup().lookupCountryCodeByIp(NORWAY_IP_ADDRESS);
        String france = new GeolocationLookup().lookupCountryCodeByIp(FRANCE_IP_ADDRESS);
        String hammersmith = new GeolocationLookup().lookupCountryCodeByIp(HAMMERSMITH_IP_ADDRESS);

        assertEquals("US", unitedStates);
        assertEquals("GB", uk);
        assertEquals("FR", france);
        assertEquals("NO", norway);
        assertEquals("GB", hammersmith);
    }

    @Test
    public void shouldDefaultToUnitedStates() throws Exception {
        assertEquals("US", new GeolocationLookup().lookupCountryCodeByIp(MADE_UP));
        assertEquals("US", new GeolocationLookup().lookupCountryCodeByIp("123"));
        assertEquals("US", new GeolocationLookup().lookupCountryCodeByIp("192.168.0.1"));
        assertEquals("US", new GeolocationLookup().lookupCountryCodeByIp("127.0.0.1"));
    }

    @Test
    public void missingDataFileShouldReturnUS() {
        final GeolocationLookup geolocationLookup = new GeolocationLookup() {
            @Override
            File getResourceFile() throws IOException {
                throw new IOException();
            }
        };
        assertThat(geolocationLookup.lookupCountryCodeByIp("123"), is(equalTo("US")));
    }

    @Test
    public void closeShouldShutdownTheService() {
        final LookupService lookupService = mock(LookupService.class);
        final GeolocationLookup underTest = withMockedService(lookupService);

        underTest.lookupCountryCodeByIp("10.9.8.1");
        underTest.close();

        verify(lookupService).close();
    }

    @Test
    public void closeShouldDeleteTheTemporaryFile() throws IOException {
        final File tempFile = File.createTempFile("GeolocationLookupTest", ".tmp");
        final GeolocationLookup underTest = withTemporaryFile(tempFile);

        underTest.lookupCountryCodeByIp("10.9.8.1");
        underTest.close();

        assertFalse(tempFile.exists());
    }

    @Test
    public void closeShouldReturnQuietlyIfTheServiceHasNotBeenCreated() {
        final LookupService lookupService = mock(LookupService.class);
        final GeolocationLookup underTest = withMockedService(lookupService);

        underTest.close();

        verifyZeroInteractions(lookupService);
    }

    private GeolocationLookup withMockedService(final LookupService lookupService) {
        return new GeolocationLookup() {
            @Override
            LookupService lookupService(final File serviceResourceFile, final int options) throws IOException {
                return lookupService;
            }
        };
    }

    private GeolocationLookup withTemporaryFile(final File temporaryFile) {
        return new GeolocationLookup() {
            @Override
            File getResourceFile() throws IOException {
                return temporaryFile;
            }
        };
    }
}
