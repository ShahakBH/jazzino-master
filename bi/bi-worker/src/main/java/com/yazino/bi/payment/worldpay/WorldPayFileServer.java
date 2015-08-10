package com.yazino.bi.payment.worldpay;

import com.jcraft.jsch.*;
import com.yazino.configuration.YazinoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class WorldPayFileServer {
    private static final int SOCKET_TIMEOUT_MS = 60000;
    private static final int SSH_CONNECTION_TIMEOUT_MS = 30000;
    private static final int DEFAULT_PORT = 22;

    private static final String PROPERTY_HOSTNAME = "payment.worldpay.sftp.hostname";
    private static final String PROPERTY_PORT = "payment.worldpay.sftp.port";
    private static final String PROPERTY_USERNAME = "payment.worldpay.sftp.username";
    private static final String PROPERTY_PASSWORD = "payment.worldpay.sftp.password";

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public WorldPayFileServer(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    public boolean fetchTo(final String filename,
                           final String destinationFilename)
            throws WorldPayFileServerException {
        notNull(filename, "filename may not be null");
        notNull(destinationFilename, "destinationFilename may not be null");

        Session session = null;
        ChannelSftp channel = null;
        try {
            session = newSession();

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            channel.get(filename, destinationFilename);
            return true;

        } catch (JSchException e) {
            throw new WorldPayFileServerException(format("Error creating SSH session to %s@%s", username(), hostname()), e);

        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            throw new WorldPayFileServerException(format("Error during SSH session to %s@%s", username(), hostname()), e);

        } finally {
            closeChannel(channel);
            closeSession(session);
        }
    }

    private void closeChannel(final ChannelSftp channel) {
        try {
            if (channel != null && channel.isConnected()) {
                channel.exit();
            }
        } catch (Exception e) {
            // ignored
        }
    }

    private void closeSession(final Session session) {
        try {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        } catch (Exception e) {
            // ignored
        }
    }

    private Session newSession() throws JSchException {
        final Session session = new JSch().getSession(username(), hostname(), port());
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("ConnectTime", Integer.toString(SSH_CONNECTION_TIMEOUT_MS));
        session.setConfig("PreferredAuthentications", "password");
        session.setTimeout(SOCKET_TIMEOUT_MS);
        final String password = yazinoConfiguration.getString(PROPERTY_PASSWORD);
        if (password != null) {
            session.setPassword(password);
        }
        session.connect();
        return session;
    }

    private String hostname() {
        final String hostname = yazinoConfiguration.getString(PROPERTY_HOSTNAME);
        if (hostname == null) {
            throw new IllegalArgumentException("No hostname is configured in property " + PROPERTY_HOSTNAME);
        }
        return hostname;
    }

    private String username() {
        final String username = yazinoConfiguration.getString(PROPERTY_USERNAME);
        if (username == null) {
            throw new IllegalArgumentException("No username is configured in property " + PROPERTY_USERNAME);
        }
        return username;
    }

    private int port() {
        return yazinoConfiguration.getInt(PROPERTY_PORT, DEFAULT_PORT);
    }

}
