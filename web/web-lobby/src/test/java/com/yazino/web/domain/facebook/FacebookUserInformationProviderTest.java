package com.yazino.web.domain.facebook;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.exception.FacebookGraphException;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;
import com.restfb.types.User;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.Partner;
import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.GeolocationLookup;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FacebookUserInformationProviderTest {
    private static final String REMOTE_IP_ADDRESS = "ip address";

    @Mock
    private SiteConfiguration siteConfig;
    @Mock
    private FacebookClientFactory clientFactory;
    @Mock
    private FacebookClient facebookClient;
    @Mock
    private HttpServletRequest request;
    @Mock
    private GeolocationLookup geolocationLookup;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private FacebookUserInformationProvider underTest;
    private FacebookUser user;

    @Before
    public void init() throws IOException {
        removeErrorFailingListAppender(FacebookUserInformationProvider.class);

        underTest = new FacebookUserInformationProvider(clientFactory, geolocationLookup, yazinoConfiguration);

        when(yazinoConfiguration.getString("strata.lobby.partnerid", "YAZINO")).thenReturn(Partner.YAZINO.name());
        given(clientFactory.getClient(anyString())).willReturn(facebookClient);
    }

    @After
    public void cleanUpAppenders() {
        removeErrorFailingListAppender(FacebookUserInformationProvider.class);
    }

    @Test
    public void shouldCreateInformationHolder() {
        givenSimpleUserLogin();

        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        assertThat(holder.getPlayerProfile().getExternalId(), is(equalTo("1234")));
        assertThat(holder.getPlayerProfile().getDisplayName(), is(equalTo("aName")));
        assertThat(holder.getFriends(), hasItems("friend1", "friend2"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowAnIllegalStateExceptionIfTheFacebookUserHasNoExternalId() {
        given(facebookClient.fetchObject("me", FacebookUser.class)).willReturn(mock(FacebookUser.class));

        underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);
    }

    @Test
    public void shouldUserFacebookIdWhenNameIsMissing() {
        user = mock(FacebookUser.class);
        given(facebookClient.fetchObject("me", FacebookUser.class)).willReturn(user);
        given(user.getId()).willReturn("1234");

        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        assertThat(holder.getPlayerProfile().getExternalId(), is(equalTo("1234")));
        assertThat(holder.getPlayerProfile().getDisplayName(), is(equalTo("1234")));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateInformationHolderWithoutFriendsWhenCanvasActionsProhibited() {
        givenSimpleUserLogin();

        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, false);

        assertThat(holder.getPlayerProfile().getExternalId(), is(equalTo("1234")));
        assertThat(holder.getFriends(), anyOf(nullValue(), is(empty())));
    }

    @Test
    public void getUserInformationHolderShouldAssignCorrectCountryFromIpRatherThanFacebookCountry() {
        givenSimpleUserLogin();

        given(geolocationLookup.lookupCountryCodeByIp(REMOTE_IP_ADDRESS)).willReturn("OZ");
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        Assert.assertThat(holder.getPlayerProfile().getCountry(), is(equalTo("OZ")));
    }

    @Test
    public void getUserInformationHolderShouldReadPartnerIdFromProperties() {
        givenSimpleUserLogin();

        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        Assert.assertThat(holder.getPlayerProfile().getPartnerId(), is(equalTo(Partner.YAZINO)));
    }

    @Test
    public void aFacebookClientFetchFailureReturnsAHolderWithAnEmptyProfile() {
        reset(facebookClient);
        given(facebookClient.fetchObject("me", FacebookUser.class)).willReturn(null);

        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        assertThat(holder.getPlayerProfile(), Matchers.is(nullValue()));
    }

    @Test
    public void failingGeoLoShouldNotBreakLogin() {
        givenSimpleUserLogin();

        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, "", true);
        Assert.assertThat(holder.getPlayerProfile().getCountry(), is(IsEqual.equalTo("US")));
    }

    private void givenSimpleUserLogin() {
        // GIVEN an identified Facebook user
        givenIdentifiedFacebookUser();

        // AND some user information is returned
        givenFriendsInformation();
    }

    private void givenIdentifiedFacebookUser() {
        user = mock(FacebookUser.class);
        given(facebookClient.fetchObject("me", FacebookUser.class)).willReturn(user);
        given(user.getId()).willReturn("1234");
        given(user.getName()).willReturn("aName");
    }

    private void givenFriendsInformation() {
        @SuppressWarnings("unchecked")
        final Connection<User> friends = mock(Connection.class);
        given(facebookClient.fetchConnection("me/friends", User.class)).willReturn(friends);
        final User friendMock = mock(User.class);
        final List<User> users = asList(friendMock, friendMock);
        given(friends.getData()).willReturn(users);
        given(friendMock.getId()).willReturn("friend1", "friend2");
    }

    @Test
    public void shouldRetrieveReferralPlayerId() {
        // GIVEN a standard user login
        givenSimpleUserLogin();

        // AND a referral information is returned by the Facebook service
        final JsonObject appRequest = mock(JsonObject.class);
        given(facebookClient.fetchObject("456", JsonObject.class)).willReturn(appRequest);
        given(appRequest.getString("data")).willReturn("789");

        // WHEN retrieving the user information and providing a referer
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", "456", REMOTE_IP_ADDRESS, true);

        // THEN the referer information is added to the user holder object
        assertThat(holder.getPlayerProfile().getReferralIdentifier(), equalTo("789"));
    }

    @Test
    public void shouldSetReferralPlayerIdToNullOnJsonException() {
        // GIVEN a standard user login
        givenSimpleUserLogin();

        // AND a referral information is returned by the Facebook service
        final JsonObject appRequest = mock(JsonObject.class);
        given(facebookClient.fetchObject("456", JsonObject.class)).willReturn(appRequest);
        given(appRequest.getString("data")).willThrow(new JsonException(""));

        // WHEN retrieving the user information and providing a referer
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", "456", REMOTE_IP_ADDRESS, true);

        // THEN the referer information is added to the user holder object
        assertThat(holder.getPlayerProfile().getReferralIdentifier(), nullValue());
    }

    @Test
    public void shouldSwallowFacebookExceptionOnObjectRemoval() {
        // GIVEN a standard user login
        givenSimpleUserLogin();

        // AND a referral information is returned by the Facebook service
        final JsonObject appRequest = mock(JsonObject.class);
        given(facebookClient.fetchObject("456", JsonObject.class)).willReturn(appRequest);
        given(appRequest.getString("data")).willReturn("789");
        given(facebookClient.deleteObject("456")).willThrow(new FacebookOAuthException("0", "", 123, 500));

        // WHEN retrieving the user information and providing a referer
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", "456", REMOTE_IP_ADDRESS, true);

        // THEN the referer information is added to the user holder object
        assertThat(holder.getPlayerProfile().getReferralIdentifier(), equalTo("789"));
    }

    @Test
    public void shouldSwallowFacebookExceptionWhenGettingFriends() {
        // GIVEN a simple user login
        givenIdentifiedFacebookUser();

        // AND the friends request fails
        given(facebookClient.fetchConnection("me/friends", User.class)).willThrow(new FacebookOAuthException("", "", 123, 500));

        // WHEN retrieving the user information
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        // THEN the information does contain all the expected data
        assertThat(holder.getFriends(), hasSize(0));
    }

    @Test
    public void shouldSwallowFacebookInvalidatedAccessTokenQuietlyExceptionWhenGettingFriends() {
        addTo(new ErrorFailingListAppender(), FacebookUserInformationProvider.class);
        givenIdentifiedFacebookUser();
        given(facebookClient.fetchConnection("me/friends", User.class))
                .willThrow(new FacebookOAuthException("Error validating access token",
                        "The session has been invalidated because the user has changed the password.", 190, 400));

        underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);
    }

    @Test
    public void shouldSwallowFacebookChangedAccessTokenQuietlyExceptionWhenGettingFriends() {
        addTo(new ErrorFailingListAppender(), FacebookUserInformationProvider.class);
        givenIdentifiedFacebookUser();
        given(facebookClient.fetchConnection("me/friends", User.class)).willThrow(new FacebookOAuthException("Error validating access token",
                "Error validating access token: Session does not match current stored session. "
                        + "This may be because the user changed the password since the time the session was created "
                        + "or Facebook has changed the session for security reasons.", 190, 400));

        underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);
    }

    @Test
    public void shouldSwallowFacebookExpiredAccessTokenQuietlyExceptionWhenGettingFriends() {
        addTo(new ErrorFailingListAppender(), FacebookUserInformationProvider.class);
        givenIdentifiedFacebookUser();
        given(facebookClient.fetchConnection("me/friends", User.class)).willThrow(new FacebookOAuthException("Error validating access token",
                "Error validating access token: Session has expired at unix time 1384343329. The current unix time is 1386077759.", 190, 400));

        underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);
    }

    @Test
    public void shouldCorrectlyParseMaleGender() {
        // GIVEN a simple user login
        givenSimpleUserLogin();

        // AND a gender string set
        given(user.getGender()).willReturn("male");

        // WHEN retrieving the user information
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        // THEN the information does contain all the expected data
        assertThat(holder.getPlayerProfile().getGender(), is(Gender.MALE));
    }

    @Test
    public void shouldCorrectlyParseFemaleGender() {
        // GIVEN a simple user login
        givenSimpleUserLogin();

        // AND a gender string set
        given(user.getGender()).willReturn("female");

        // WHEN retrieving the user information
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        // THEN the information does contain all the expected data
        assertThat(holder.getPlayerProfile().getGender(), is(Gender.FEMALE));
    }

    @Test
    public void shouldFetchUserPicture() {
        // GIVEN a simple user login
        givenSimpleUserLogin();

        // WHEN retrieving the user information
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        // THEN the information does contain all the expected data
        assertThat(holder.getAvatarUrl(), is("https://graph.facebook.com/1234/picture"));
    }

    @Test
    public void shouldSwallowOAuthExceptionAndReturnNullProfile() {
        // GIVEN a failed user login
        given(facebookClient.fetchObject("me", FacebookUser.class)).willThrow(new FacebookOAuthException("", "", 123, 500));

        // WHEN retrieving the user information
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        // THEN the information does contain all the expected data
        assertThat(holder.getPlayerProfile(), nullValue());
    }

    @Test
    public void shouldSwallowFacebookExceptionAndReturnNullProfile() {
        // GIVEN a failed user login
        given(facebookClient.fetchObject("me", FacebookUser.class)).willThrow(new FacebookGraphException("", "", 123));

        // WHEN retrieving the user information
        final PlayerInformationHolder holder = underTest.getUserInformationHolder("token", null, REMOTE_IP_ADDRESS, true);

        // THEN the information does contain all the expected data
        assertThat(holder.getPlayerProfile(), nullValue());
    }

    private void removeErrorFailingListAppender(final Class loggerName) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(loggerName);

        logger.detachAppender("ErrorFailingListAppender");
    }

    private void addTo(final ListAppender<ILoggingEvent> logAppender,
                       final Class loggerName) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(loggerName);

        logAppender.setContext(logger.getLoggerContext());
        logAppender.setName("ErrorFailingListAppender");
        logger.addAppender(logAppender);

        logAppender.start();
    }

    private static class ErrorFailingListAppender extends ListAppender<ILoggingEvent> {
        @Override
        protected void append(final ILoggingEvent eventObject) {
            if (eventObject.getLevel() == Level.ERROR) {
                fail("Unexpected error logged: " + eventObject.getMessage());
            }
            super.append(eventObject);
        }
    }
}
