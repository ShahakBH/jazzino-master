package com.yazino.yaps;

import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Provides access to Apples feedback service.
 */
public class FeedbackService {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackService.class);

    private final String mBundle;
    private final AppleSocketFactory mSocketFactory;
    private final MobileDeviceService mobileDeviceDao;
    private FeedbackTransformer mTransformer = new FeedbackTransformer();
    private long mReadTimeout = TimeUnit.SECONDS.toMillis(5);

    public FeedbackService(String bundle, final AppleSocketFactory socketFactory, final MobileDeviceService mobileDeviceDao) {
        notNull(bundle);
        notNull(socketFactory);
        notNull(mobileDeviceDao);
        mBundle = bundle;
        mSocketFactory = socketFactory;
        this.mobileDeviceDao = mobileDeviceDao;
    }

    public void readFeedback() throws Exception {
        final Set<Feedback> feedbacks = new HashSet<Feedback>();
        Socket socket = null;
        try {
            LOG.debug("Reading feedback for bundle [{}]", mBundle);

            socket = mSocketFactory.newSocket();
            socket.setSoTimeout(Long.valueOf(mReadTimeout).intValue());
            final InputStream inputStream = socket.getInputStream();
            final byte[] bytes = new byte[38];
            while (inputStream.read(bytes) > 0) {

                try {
                    final Feedback feedback = mTransformer.fromBytes(bytes);
                    feedbacks.add(feedback);
                    LOG.debug("Read feedback [{}]", feedback);

                } catch (MessageTransformationException e) {
                    LOG.warn("Failed to transform message [{}]", new String(bytes), e);
                }
            }
            LOG.debug("Finished reading feedback for bundle [{}]", mBundle);
        } catch (SocketTimeoutException e) {
            LOG.debug("Timed out waiting for feedback for bundle {} from Apple", mBundle);
        } finally {
            QuietCloser.closeQuietly(socket);
            removeDevices(mBundle, feedbacks);
        }
    }

    private void removeDevices(String bundle, Set<Feedback> feedbacks) {
        if (!feedbacks.isEmpty()) {
            LOG.info("Removing [{}] devices for bundle [{}]", feedbacks.size(), bundle);
            for (Feedback feedback : feedbacks) {
                mobileDeviceDao.deregisterToken(Platform.IOS, feedback.getDeviceToken());
            }
        } else {
            LOG.info("No devices to be removed for gameType [{}]", bundle);
        }
    }

    final void setTransformer(final FeedbackTransformer transformer) {
        notNull(transformer, "transformer was null");
        mTransformer = transformer;
    }
}
