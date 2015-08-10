package com.yazino.yaps;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

import static org.apache.commons.lang3.Validate.notBlank;

/**
 * Configuration for connecting to a secure socket.
 */
public class SecurityConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    private static final char[] KEY_PASSWORD = "s1gn4tur3".toCharArray();
    private static final String PROTOCOL = "SSL";
    private static final String KEY_STORE_TYPE = "PKCS12";
    private static final String ALGORITHM = "SunX509";

    private final String certificateDirectory;
    private final String certificateName;

    private SSLContext sslContext;

    public SecurityConfig(final String certificateDirectory,
                          final String certificateName) {
        notBlank(certificateDirectory, "certificateDirectory may not be blank");
        notBlank(certificateName, "certificateName may not be blank");

        this.certificateDirectory = certificateDirectory;
        this.certificateName = certificateName;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }

    @PostConstruct
    public void initialise() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException,
            UnrecoverableKeyException, KeyManagementException {
        LOG.info("Loading config for certificate: {}", certificateName);

        final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        try (InputStream stream = certificateInputStream()) {
            keyStore.load(stream, KEY_PASSWORD);
        }

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(ALGORITHM);
        keyManagerFactory.init(keyStore, KEY_PASSWORD);

        sslContext = SSLContext.getInstance(PROTOCOL);
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
    }

    private InputStream certificateInputStream() throws FileNotFoundException {
        final File certificateFile = new File(certificateDirectory, certificateName);
        final InputStream stream;
        if (certificateFile.exists()) {
            LOG.debug("Loading certificate from FS: {}", certificateFile);
            stream = new BufferedInputStream(new FileInputStream(certificateFile));

        } else {
            LOG.debug("Loading certificate from classpath: {}", certificateName);
            stream = SecurityConfig.class.getResourceAsStream("/" + certificateName);
        }
        return stream;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this);
    }

}
