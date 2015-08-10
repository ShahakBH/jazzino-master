package com.yazino.web.session;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.math.BigDecimal;

import static com.yazino.platform.Partner.YAZINO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LobbySessionReferenceTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(234234);
    public static final AuthProvider PROVIDER = AuthProvider.YAZINO;

    @Test
    public void aLobbySessionReferenceForASessionWillContainThePlayerId() {
        final LobbySessionReference reference = new LobbySessionReference(aLobbySession());

        assertThat(reference.getPlayerId(), is(equalTo(PLAYER_ID)));
    }

    @Test
    public void aLobbySessionReferenceForASessionWillContainTheSessionKey() {
        final LobbySessionReference reference = new LobbySessionReference(aLobbySession());

        assertThat(reference.getSessionKey(), is(equalTo("aSessionKey")));
    }

    @Test
    public void aLobbySessionReferenceForASessionWillContainThePlatform() {
        final LobbySessionReference reference = new LobbySessionReference(aLobbySession());

        assertThat(reference.getPlatform(), is(equalTo(Platform.ANDROID)));
    }

    @Test
    public void aLobbySessionReferenceForASessionWillContainTheProviderName() {
        final LobbySessionReference reference = new LobbySessionReference(aLobbySession());

        assertThat(reference.getAuthProvider(), is(equalTo(PROVIDER)));
    }

    @Test(expected = NullPointerException.class)
    public void aLobbySessionReferenceCannotBeConstructedFromANullSession() {
        new LobbySessionReference(null);
    }

    @Test
    public void anEncodedLobbySessionReferenceCanBeDecodedToTheSameRequest() {
        final LobbySessionReference reference = new LobbySessionReference(aLobbySession());

        final String encoded = reference.encode();

        assertThat(LobbySessionReference.fromEncodedSession(encoded), is(equalTo(reference)));
    }

    @Test
    public void aLegacyEncodedLobbySessionReferenceCanBeDecodedToTheSameRequest() {
        final LobbySessionReference reference = new LobbySessionReference(aLobbySessionWith(PLAYER_ID, "aSessionkey", null, PROVIDER));

        final String encoded = reference.encode();

        assertThat(LobbySessionReference.fromEncodedSession(encoded),
                is(equalTo(new LobbySessionReference(aLobbySessionWith(PLAYER_ID, "aSessionkey", Platform.WEB, PROVIDER)))));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void anEncodedLobbySessionReferenceCanBeDecoded() {
        final LobbySessionReference decodedSessionReference = LobbySessionReference.fromEncodedSession(encode(PLAYER_ID + "+aSessionKey+IOS+Yahoo!"));

        assertThat(decodedSessionReference,
                is(equalTo(new LobbySessionReference(aLobbySessionWith(PLAYER_ID, "aSessionKey", Platform.IOS, AuthProvider.YAHOO)))));
    }

    @Test
    public void aNullEncodedLobbySessionReferenceWillBeDecodedToNull() {
        final LobbySessionReference decodedSessionReference = LobbySessionReference.fromEncodedSession(null);

        assertThat(decodedSessionReference, is(nullValue()));
    }

    @Test
    public void anUndelimitedEncodedLobbySessionReferenceWillBeDecodedToNull() {
        final LobbySessionReference decodedSessionReference = LobbySessionReference.fromEncodedSession("anUndelimitedString");

        assertThat(decodedSessionReference, is(nullValue()));
    }

    @Test
    public void anOverlyLongEncodedLobbySessionReferenceWillBeDecodedToNull() {
        final LobbySessionReference decodedSessionReference = LobbySessionReference.fromEncodedSession(PLAYER_ID + "+aSessionKey+WEB+something");

        assertThat(decodedSessionReference, is(nullValue()));
    }

    @Test
    public void anEncodedLobbySessionReferenceWithoutAtLeastTwoFieldsWillBeDecodedToNull() {
        final LobbySessionReference decodedSessionReference = LobbySessionReference.fromEncodedSession(PLAYER_ID.toPlainString());

        assertThat(decodedSessionReference, is(nullValue()));
    }

    @Test
    public void anEncodedLobbySessionReferenceWithAnInvalidPlatformWillBeDecodedToNull() {
        final LobbySessionReference decodedSessionReference = LobbySessionReference.fromEncodedSession(PLAYER_ID + "+aSessionKey+NADA");

        assertThat(decodedSessionReference, is(nullValue()));
    }

    @Test
    public void anEncodedLobbySessionReferenceWithAMissingPlatformWillBeDecodedToUseWeb() {
        final LobbySessionReference decodedSessionReference = LobbySessionReference.fromEncodedSession(encode(PLAYER_ID + "+aSessionKey"));

        assertThat(decodedSessionReference,
                is(equalTo(new LobbySessionReference(aLobbySessionWith(PLAYER_ID, "aSessionKey", Platform.WEB, AuthProvider.YAZINO)))));
    }

    private String encode(final String toEncode) {
        return new String(Base64.encodeBase64(toEncode.getBytes()));
    }

    private LobbySession aLobbySession() {
        return aLobbySessionWith(PLAYER_ID, "aSessionKey", Platform.ANDROID, PROVIDER);
    }

    private LobbySession aLobbySessionWith(final BigDecimal playerId,
                                           final String sessionKey,
                                           final Platform platform,
                                           final AuthProvider providerName) {
        return new LobbySession(BigDecimal.valueOf(3141592), playerId, "aPlayerName", sessionKey, YAZINO,
                "aPictureUrl", "anEmailAddress", null, false, platform, providerName);
    }

    @Test
    public void shouldSupportReallyLongSessionId() {
        LobbySession lobbySession = aLobbySessionWith(new BigDecimal("12345678912345"), "123aksjdlakjdlksajdlkajsdlkjaslkdas", Platform.ANDROID, AuthProvider.FACEBOOK);
        assertThat(new LobbySessionReference(lobbySession).encode(), not(containsString("\n")));
    }

}
