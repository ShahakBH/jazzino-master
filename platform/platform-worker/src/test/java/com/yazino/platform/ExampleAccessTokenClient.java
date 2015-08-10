package com.yazino.platform;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class ExampleAccessTokenClient {
    public static void main(String[] args) throws Exception {
        String certificateFilePath = args[0];
        String certificatePassword = args[1];
        String tangoCaCertPath = args[2];
        String tangoServerHost = args[3];
        String tangoServerPort = args[4];
        int port = Integer.parseInt(tangoServerPort);
        String base64AccessToken = args[5];
        validateAccessToken(certificateFilePath, certificatePassword, tangoCaCertPath,
                tangoServerHost,
                port, base64AccessToken);
    }

    /**
     * Validates the Access Token.
     *
     * @param certificateFilePath the file path to the PKCS12 client certificate
     * @param certificatePassword the password used to open the PKCS12 client
     *                            certificate
     * @param tangoCaCertPath     the file path to the Tango public CA certificate
     * @param tangoServerHost     the fully qualified host name of the tango SSO service
     * @param tangoServerPort     the port number for the tango SSO service
     * @param base64AccessToken   the base64 encoded Access Token acquired from the
     *                            ￼￼￼Tango SDK
     */
    public static void validateAccessToken(String certificateFilePath, String
            certificatePassword,
                                           String tangoCaCertPath, String tangoServerHost, int tangoServerPort,
                                           String base64AccessToken) {
        // Load the provided certificate into a keystore
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(new File(certificateFilePath)),
                    certificatePassword.toCharArray());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }
        // Create a key store with the public Tango CA certificate
        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null, null);
            CertificateFactory certFact = CertificateFactory.getInstance("X509");
            Certificate cert = certFact.generateCertificate(new FileInputStream(new
                    File(tangoCaCertPath)));
            trustStore.setCertificateEntry("tango_ca", cert);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }
        // Prepare the key managers including one for the provided client certificate
        KeyManager[] keyManagers = null;
        try {
            final char[] keyPassword = certificatePassword.toCharArray();
            KeyManagerFactory kmFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmFactory.init(keyStore, keyPassword);
            keyManagers = kmFactory.getKeyManagers();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }
        // Prepare the trust managers to include one for the configured Tango CA
        TrustManager[] trustManagers = null;
        try {
            TrustManagerFactory tmFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(trustStore);
            trustManagers = tmFactory.getTrustManagers();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }
        // Establish SSL context with the appropriate certificates
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return;
        }
// Custom socket factory that wraps the one obtained from the prepared SSL Context
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        ProtocolSocketFactory socketFactory = new ProtocolSocketFactory() {
            @Override
            public Socket createSocket(String host, int port) throws IOException,
                    UnknownHostException {
                return sslSocketFactory.createSocket(host, port);
            }

            @Override
            public Socket createSocket(String host, int port, InetAddress localHost,
                                       int localPort) throws IOException,
                    UnknownHostException {
                return sslSocketFactory.createSocket(host, port, localHost,
                        localPort);
            }

            @Override
            public Socket createSocket(String host, int port, InetAddress localHost,
                                       int localPort,
                                       HttpConnectionParams params) throws IOException,
                    UnknownHostException, ConnectTimeoutException {
                return sslSocketFactory.createSocket(host, port, localHost, localPort);
            }
        };
// Issue the post request to the Tango SDK SSO Server
        Protocol https = new Protocol("https", socketFactory, tangoServerPort);
        HttpClient httpclient = new HttpClient();
        httpclient.getHostConfiguration().setHost(tangoServerHost, tangoServerPort, https);
        PostMethod post = new PostMethod("/sdkSso/v1/access.json");
        String rsp = null;
        try {
            String accessTokenJson = "{\"AccessTokenRequest\":{\"AccessToken\":\"" +
                    base64AccessToken + "\"}}";
            post.setRequestEntity(new StringRequestEntity(accessTokenJson,
                    "application/json", "UTF-8"));
            httpclient.executeMethod(post);
            if (post.getStatusCode() != 200) {
                System.err.println("HTTP Request failure: " + post.getStatusLine());
            }
            InputStream inStream = post.getResponseBodyAsStream();
            InputStreamReader reader = new InputStreamReader(inStream);
            BufferedReader in = new BufferedReader(reader);
            rsp = in.readLine();
            in.close();
        } catch (final Exception exc) {
            exc.printStackTrace(System.err);
            return;
        } finally {
            post.releaseConnection();
        }
        if (null == rsp) {
            System.err.println("No response");
            return;
        }
        // Extract the authentication status from the response
        int statusStartIndex = rsp.indexOf("Status");
        if (statusStartIndex < 0) {
            System.err.println("No Status in response: " + rsp);
            return;
        }
        statusStartIndex += 9;
        int statusEndIndex = rsp.indexOf('"', statusStartIndex);
        if (statusEndIndex < 0) {
            System.err.println("Expected a terminating double quote for the Status value: " + rsp);
            return;
        }
        final String status = rsp.substring(statusStartIndex, statusEndIndex);
        if (!"AUTH_SUCCESS".equals(status)) {
            System.err.println("Authentication failed: " + rsp);
            return;
        }
        // Extract the Tango ID from the response
        int idIndex = rsp.indexOf("TangoId");
        if (idIndex < 0) {
            System.err.println("No TangoId found in response:" + rsp);
            return;
        }
        idIndex += 10;
        String id = rsp.substring(idIndex, idIndex + 22);
        System.out.println("TangoId: " + id);
    }
}
