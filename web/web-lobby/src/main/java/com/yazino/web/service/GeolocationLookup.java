package com.yazino.web.service;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.*;


@Service("geolocationLookup")
public class GeolocationLookup {
    private static final Logger LOG = LoggerFactory.getLogger(GeolocationLookup.class);

    private static final String SOURCE_PATH = "GeoIP.dat";

    private final Object lookupServiceLock = new Object();

    private LookupService lookupService;
    private File resourceFile;

    private LookupService getService() throws IOException {
        synchronized (lookupServiceLock) {
            if (lookupService == null) {
                try {
                    resourceFile = getResourceFile();
                    lookupService = lookupService(resourceFile,
                            LookupService.GEOIP_MEMORY_CACHE | LookupService.GEOIP_CHECK_CACHE);

                } catch (IOException e) {
                    LOG.error("Cannot find {} used for GeoIP lookup.", SOURCE_PATH);
                    throw e;
                }
            }
            return lookupService;
        }
    }

    LookupService lookupService(final File serviceResourceFile, final int options) throws IOException {
        return new LookupService(serviceResourceFile, options);
    }

    File getResourceFile() throws IOException {
        final File tempFile = File.createTempFile("GeoIP", ".dat");

        final BufferedInputStream resourceAsStream = new BufferedInputStream(
                new ClassPathResource(SOURCE_PATH).getInputStream());
        final BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(tempFile));
        try {
            int nextchar;
            while ((nextchar = resourceAsStream.read()) != -1) {
                writer.write(nextchar);
            }

            writer.flush();

        } finally {

            writer.close();
        }
        tempFile.deleteOnExit();
        return tempFile;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @PreDestroy
    public void close() {
        synchronized (lookupServiceLock) {
            if (lookupService != null) {
                lookupService.close();
                lookupService = null;
            }

            if (resourceFile != null && resourceFile.exists()) {
                resourceFile.delete();
            }
        }
    }

    public String lookupCountryCodeByIp(final String ipAddress) {
        final Country country;
        try {
            country = getService().getCountry(ipAddress);
        } catch (IOException e) {
            return "US";
        }
        if (country == null || "--".equals(country.getCode())) {
            return "US";
        }
        return country.getCode();
    }


}
