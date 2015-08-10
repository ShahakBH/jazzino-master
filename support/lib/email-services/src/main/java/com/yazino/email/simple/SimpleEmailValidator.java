package com.yazino.email.simple;

import com.yazino.email.EmailValidator;
import com.yazino.email.EmailVerificationResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import javax.naming.NamingException;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static com.yazino.email.EmailVerificationStatus.*;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * A service to attempt to validate the existence of an email address.
 * <p>
 * We do this by querying the SMTP server. Most of the big providers don't support
 * the verify command. However, many will reject emails to invalid addresses. Therefore
 * we attempt to this point and then disconnected.
 * </p>
 * <p>
 * If it's not supported, it works, or weird stuff happens, you should consider the address as
 * a possible address. There's no way to guarantee it.
 * </p>
 */
public class SimpleEmailValidator implements EmailValidator {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleEmailValidator.class);

    private static final int SMTP_PORT = 25;
    private static final int DEFAULT_SOCKET_TIMEOUT = 5000;
    private static final int CONNECTION_TIMEOUT = 1000;

    private static final int SERVICE_READY = 220;
    private static final int OKAY = 250;
    private static final int MAILBOX_UNAVAILABLE = 550;
    private static final int SERVICE_NOT_UNAVAILABLE = 421;
    private static final int RESPONSE_CODE_LENGTH = 3;

    private final String fromAddress;
    private final String fromHost;

    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

    /**
     * Create a validator.
     */
    public SimpleEmailValidator() {
        fromHost = getLocalHostName();
        fromAddress = "contact@" + fromHost;

        LOG.debug("Created validator with fromHost={}, fromAddress={}", fromHost, fromAddress);
    }

    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();

        } catch (UnknownHostException e) {
            return "verify.london.yazino.com";
        }
    }

    /**
     * Attempt to determine if an address is valid.
     *
     * @param address the email address to test. May not be null.
     * @return false if invalid; true if it may be valid.
     */
    public EmailVerificationResult validate(final String address) {
        notNull(address, "address may not be null");

        final int hostIndex = address.indexOf("@");
        if (hostIndex == -1) {
            LOG.debug("Email address is malformed: {}", address);
            return new EmailVerificationResult(address, MALFORMED);
        }

        final String hostname = address.substring(hostIndex + 1);
        if (StringUtils.isBlank(hostname)) {
            LOG.debug("Email address is malformed: {}", address);
            return new EmailVerificationResult(address, MALFORMED);
        }

        return checkMxServerExistsAndAccepts(address, hostname);
    }

    void setSocketTimeout(final int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    private EmailVerificationResult checkMxServerExistsAndAccepts(final String address,
                                                                  final String hostname) {
        Socket smtpSocket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            final String mxServer = getMxServer(hostname);
            if (mxServer == null) {
                LOG.debug("Failed to find a MX server for {}", address);
                return new EmailVerificationResult(address, INVALID);
            }

            smtpSocket = connectTo(mxServer);

            reader = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(smtpSocket.getOutputStream()));

            final int connectResponse = readResponse(reader);
            if (connectResponse != SERVICE_READY) {
                LOG.debug("Could not connect to SMTP server at {}, received code {}; cannot verify {}",
                        mxServer, connectResponse, address);
                return new EmailVerificationResult(address, UNKNOWN_TEMPORARY);
            }

            send(writer, String.format("EHLO %s", fromHost));
            final int ehloResponse = readResponse(reader);
            if (ehloResponse == SERVICE_NOT_UNAVAILABLE) {
                LOG.debug("Server {} is not not available, cannot verify {}", mxServer, address);
                return new EmailVerificationResult(address, UNKNOWN_TEMPORARY);

            } else if (ehloResponse != OKAY) {
                LOG.debug("Server {} is not ESMTP, cannot verify {} (response was {})", mxServer, address, ehloResponse);
                return new EmailVerificationResult(address, UNKNOWN);
            }

            send(writer, String.format("MAIL FROM: <%s>", fromAddress));
            if (readResponse(reader) != OKAY) {
                LOG.debug("Sender {} was rejected by {}, cannot verify {}", fromAddress, mxServer, address);
                return new EmailVerificationResult(address, UNKNOWN);
            }

            send(writer, String.format("RCPT TO: <%s>", address));
            if (readResponse(reader) == MAILBOX_UNAVAILABLE) {
                return new EmailVerificationResult(address, INVALID);
            }

            return new EmailVerificationResult(address, VALID);

        } catch (NamingException e) {
            LOG.debug("DNS lookup failed for {}, {} is invalid", hostname, address, e);
            return new EmailVerificationResult(address, INVALID);

        } catch (IOException e) {
            LOG.debug("Connection to MX server failed for {}, cannot verify {}", hostname, address, e);
            return new EmailVerificationResult(address, UNKNOWN_TEMPORARY);

        } catch (Exception e) {
            LOG.error("Email verification failed, cannot verify {}", address, e);
            return new EmailVerificationResult(address, UNKNOWN_TEMPORARY);

        } finally {
            closeConnection(reader, writer);
            closeQuietly(reader, writer);
            closeQuietly(smtpSocket);
        }
    }

    private Socket connectTo(final String mxServer) throws IOException {
        final Socket smtpSocket = new Socket();
        smtpSocket.setSoTimeout(socketTimeout);
        smtpSocket.connect(new InetSocketAddress(mxServer, SMTP_PORT), CONNECTION_TIMEOUT);
        return smtpSocket;
    }

    private void closeQuietly(final Closeable... closeables) {
        if (closeables == null) {
            return;
        }

        for (final Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    private void closeQuietly(final Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    private void closeConnection(final BufferedReader reader,
                                 final BufferedWriter writer) {
        if (reader == null || writer == null) {
            return;
        }

        try {
            send(writer, "RSET");
            readResponse(reader);
            send(writer, "QUIT");
            readResponse(reader);

        } catch (IOException e) {
            // ignored
        }
    }

    private static int readResponse(final BufferedReader in) throws IOException {
        String line;
        int responseCode = 0;

        while ((line = in.readLine()) != null) {
            if (line.length() >= RESPONSE_CODE_LENGTH) {
                LOG.debug("SMTP: <-- {}", line);

                try {
                    final String prefix = line.substring(0, RESPONSE_CODE_LENGTH);
                    responseCode = Integer.parseInt(prefix);
                } catch (Exception ex) {
                    responseCode = -1;
                }

                if (line.charAt(RESPONSE_CODE_LENGTH) != '-') {
                    break;
                }
            }
        }

        return responseCode;
    }

    private static void send(final BufferedWriter writer,
                             final String command)
            throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();

        LOG.debug("SMTP: --> {}", command);
    }

    private String getMxServer(final String hostName) throws NamingException {
        String mxServer = checkMXRecordsFor(hostName);

        if (mxServer == null) {
            LOG.debug("No MX records returned for {}", hostName);
            mxServer = checkRecordFor(hostName, Type.A);
        }

        if (mxServer == null) {
            LOG.debug("No A records returned for {}", hostName);
            mxServer = checkRecordFor(hostName, Type.AAAA);
        }

        if (mxServer == null) {
            LOG.debug("MX/A/AAAA records missing or invalid for: {}", hostName);
            return null;
        }

        return mxServer;
    }

    private String checkRecordFor(final String hostName, final int recordType) {
        try {
            final Record[] records = new Lookup(hostName, recordType).run();
            if (records != null && records.length > 0) {
                return records[0].getName().toString();
            }
            return null;

        } catch (TextParseException e) {
            LOG.error("DNS lookup of record {} failed for {}", recordType, hostName, e);
            return null;
        }
    }

    private String checkMXRecordsFor(final String hostName) {
        try {
            final Record[] mxRecords = new Lookup(hostName, Type.MX).run();
            if (mxRecords == null) {
                return null;
            }

            String mxServer = null;
            int mxPriority = -1;
            for (final Record mxRecord : mxRecords) {
                final MXRecord mx = (MXRecord) mxRecord;
                if (mxPriority == -1 || mx.getPriority() < mxPriority) {
                    mxServer = mx.getTarget().toString();
                    mxPriority = mx.getPriority();
                }
            }
            return mxServer;

        } catch (TextParseException e) {
            LOG.error("MX lookup failed for {}", hostName, e);
            return null;
        }
    }

}
