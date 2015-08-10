package com.yazino.engagement.facebook;

import com.restfb.Facebook;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.FacebookType;
import com.yazino.engagement.FacebookMessageType;
import com.yazino.engagement.campaign.AccessTokenException;
import com.yazino.engagement.campaign.AppRequestExternalReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.facebook.FacebookAppToUserRequestType;
import strata.server.lobby.api.facebook.FacebookDataContainer;
import strata.server.lobby.api.facebook.FacebookDataContainerBuilder;

/**
 * Sends application requests to facebook users.
 *
 * @see <a href="https://developers.facebook.com/docs/reference/api/user/#apprequests">facebook apprequest</a>
 */
@Service
public class FacebookRequestSender {

    public static final String APP_USER_REQUEST_FORMAT_STRING = "%s/apprequests";
    public static final String APP_USER_NOTIFICATION_FORMAT_STRING = "%s/notifications";


    private static final Logger LOG = LoggerFactory.getLogger(FacebookRequestSender.class);
    private static final int HTTP_NOT_FOUND = 404;
    public static final int MAX_MESSAGE_LENGTH_ALLOWED_BY_FACEBOOK = 180;
    public static final int OAUTH_ERROR_TOS_NOT_ACCEPTED = 200;
    private final FacebookAccessTokenService facebookAccessTokenService;
    private final FacebookClientFactory facebookClientFactory;

    @Autowired
    public FacebookRequestSender(final FacebookAccessTokenService facebookAccessTokenService,
                                 final FacebookClientFactory facebookClientFactory) {
        this.facebookAccessTokenService = facebookAccessTokenService;
        this.facebookClientFactory = facebookClientFactory;
    }

    /*
     Idea: to restrict sending request/notifications only to those users who have accepted TOS for sending app.
     We currently do not track which users have accepted TOS and so we optimistically send messages to all users
     resulting in a large number of logged rejections "(#200) All users in param ids must have accepted TOS".

     Solution:
      - add a table (strataproddw) indicating which apps' TOS the player has accepted/not accepted, where known either way
      - before sending, lookup whether player has accepted TOS for the sending app
      - if not known, lookup using Graph API and update table accordingly
      - send or do not send accordingly

      The relevant queries are:
       - second, lookup the permission: https://graph.facebook.com/EXTERNAL_ID_FOR_PLAYER/permissions?fields=installed&access_token=ACCESS_TOKEN
         (you need to make a second call to facebookAccessTokenService.fetchApplicationAccessToken since access tokens should only be used once)

      Note: we need to update table when TOS authorisation changes:
       - A: if send is rejected for non-acceptance of TOS then update the table as the user must have de-authorised the app
       - B: when the user authorises an app, we need to update the table but we haven't yet figured out a trigger for this update
     */
    public FacebookResponse sendRequest(FacebookMessageType messageType, final FacebookAppRequestEnvelope envelopeFacebook) {
        String accessToken;
        try {
            accessToken = facebookAccessTokenService.fetchApplicationAccessToken(envelopeFacebook.getGameType());
        } catch (AccessTokenException e) {
            LOG.warn("Failed to get facebook access token for {} app.", envelopeFacebook.getGameType(), e);
            return null;
        }

        final FacebookClient fbClient = facebookClientFactory.getClient(accessToken);
        try {
            switch (messageType) {
                case APP_TO_USER_REQUEST:
                    return publishAppRequest(envelopeFacebook, fbClient);
                case APP_TO_USER_NOTIFICATION:
                    return publishAppNotification(envelopeFacebook, fbClient);
                default:
                    throw new IllegalArgumentException(String.format("Unsupported message type \"%s\"", messageType));
            }
        } catch (FacebookOAuthException foe) {
            logOAuthException(envelopeFacebook, foe);
        } catch (Exception e) {
            logException(envelopeFacebook, e);
        }
        return new FacebookResponse(FacebookAppToUserRequestStatus.FAILED, null);
    }

    private FacebookResponse publishAppNotification(FacebookAppRequestEnvelope envelopeFacebook, FacebookClient fbClient) {
        if (envelopeFacebook.getMessage().length() > MAX_MESSAGE_LENGTH_ALLOWED_BY_FACEBOOK) {
            throw new IllegalArgumentException(String.format("Message length (%d) exceeds the maximum allowed by Facebook (%d).",
                    envelopeFacebook.getMessage().length(), MAX_MESSAGE_LENGTH_ALLOWED_BY_FACEBOOK));
        }
        final AppNotificationResponse response = fbClient.publish(String.format(APP_USER_NOTIFICATION_FORMAT_STRING,
                        envelopeFacebook.getExternalId()),
                AppNotificationResponse.class,
                Parameter.with("template", envelopeFacebook.getMessage()),
                Parameter.with("href", envelopeFacebook.getData()));

        return new FacebookResponse(response.getStatus(), null);
    }

    private FacebookResponse publishAppRequest(final FacebookAppRequestEnvelope envelopeFacebook,
                                               final FacebookClient fbClient) {
        final FacebookDataContainer facebookDataContainer = new FacebookDataContainerBuilder()
                .withType(FacebookAppToUserRequestType.Engagement)
                .withTrackingRef(envelopeFacebook.getData()).build();
        final AppRequestResponse response = fbClient.publish(String.format(APP_USER_REQUEST_FORMAT_STRING,
                        envelopeFacebook.getExternalId()),
                AppRequestResponse.class,
                Parameter.with("message", envelopeFacebook.getMessage()),
                Parameter.with("data", facebookDataContainer.toJsonString()));
        return new FacebookResponse(FacebookAppToUserRequestStatus.SENT, response.getRequest());
    }

    private void logException(final FacebookAppRequestEnvelope envelopeFacebook, final Exception e) {
        LOG.warn("Failed to send app request to facebook for facebook id {}, envelopeFacebook {}",
                new Object[]{envelopeFacebook.getExternalId(),
                        envelopeFacebook.getMessage()}, e);
    }

    private void logOAuthException(FacebookAppRequestEnvelope envelopeFacebook, FacebookOAuthException e) {
        final String format = "Failed to send app request. Bad access token for facebook id {}, envelopeFacebook {}";
        final Object[] args = {envelopeFacebook.getExternalId(), envelopeFacebook.getMessage()};
        if (e.getErrorCode() == OAUTH_ERROR_TOS_NOT_ACCEPTED) {
            LOG.debug(format, args, e);
        } else {
            LOG.info(format, args, e);
        }
    }


    public void deleteRequest(final AppRequestExternalReference deleteRequest) {
        String accessToken;
        try {
            accessToken = facebookAccessTokenService.fetchApplicationAccessToken(deleteRequest.getGameType());
        } catch (AccessTokenException e) {
            LOG.warn("Failed to get facebook access token for {} app for delete request.", deleteRequest.getGameType(), e);
            return;
        }
        try {
            final FacebookClient fbClient = facebookClientFactory.getClient(accessToken);
            fbClient.deleteObject(buildAppRequestIdString(deleteRequest));

        } catch (FacebookOAuthException e) {
            if (e.getErrorCode() != null && e.getHttpStatusCode() == HTTP_NOT_FOUND) {
                LOG.info("Attempted to delete non-existent object: {}; error was {}:{}:{}",
                        deleteRequest, e, e.getErrorCode(), e.getErrorType(), e.getErrorMessage());
            } else {
                LOG.warn("due to OAUTH Error failed to delete app request: {}", deleteRequest, e);
            }
        } catch (Exception e) {
            LOG.error("Failed to delete request: {}", deleteRequest, e);
        }
    }

    public static String buildAppRequestIdString(final AppRequestExternalReference deleteRequest) {
        return deleteRequest.getExternalReference() + "_" + deleteRequest.getExternalId();
    }

    public static class AppRequestResponse extends FacebookType {
        private static final long serialVersionUID = 1L;

        @Facebook
        private String request;

        @Facebook
        private String to;

        public String getRequest() {
            return request;
        }

        public void setRequest(final String request) {
            this.request = request;
        }

        public String getTo() {
            return to;
        }

        public void setTo(final String to) {
            this.to = to;
        }
    }

    public static class AppNotificationResponse extends FacebookType {
        private static final long serialVersionUID = 1L;

        @Facebook
        private Boolean success;
        private FacebookAppToUserRequestStatus status;

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public FacebookAppToUserRequestStatus getStatus() {
            if (success) {
                return FacebookAppToUserRequestStatus.SENT;
            } else {
                return FacebookAppToUserRequestStatus.FAILED;
            }
        }
    }

}
