package com.yazino.web.controller;

import com.yazino.platform.Partner;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.SymmetricEncryptor;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TangoPlayerInformationProviderTest {

    private static final String ENCRYPTED_DATA = "scrambledeggs";
    private SymmetricEncryptor encryptor = mock(SymmetricEncryptor.class);
    private TangoPlayerInformationProvider underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new TangoPlayerInformationProvider(encryptor, new JsonHelper());

    }

    @Test
    public void jsonValidationShouldCheckForMissingDisplayname() throws IOException, GeneralSecurityException {
        final String decryptedJson = "{\"displayName\":\"\",\"accountId\":\"666\",\"avatarUrl\":\"http://your.mum/so/fat.gif\"}";
        when(encryptor.decrypt(ENCRYPTED_DATA)).thenReturn(decryptedJson);
        try {
            underTest.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("DisplayName"));
        }
    }

    @Test
    public void jsonValidationShouldCheckForMissingAccountId() throws IOException, GeneralSecurityException {
        final String decryptedJson = "{\"displayName\":\"123\",\"accountId\":\"\",\"avatarUrl\":\"http://your.mum/so/fat.gif\"}";
        when(encryptor.decrypt(ENCRYPTED_DATA)).thenReturn(decryptedJson);
        try {
            underTest.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("AccountId"));
        }
    }

    @Test
    public void getPlayerInformationShouldFillPlayer() throws GeneralSecurityException {
        final String decryptedJson = "{\"displayName\":\"Jim\",\"accountId\":\"666\",\"avatarUrl\":\"http://your.mum/so/fat.gif\"}";
        when(encryptor.decrypt(ENCRYPTED_DATA)).thenReturn(decryptedJson);
        final PlayerInformationHolder playerInfo = underTest.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA);
        assertThat(playerInfo, equalTo(setupPlayerHolder()));
    }

    @Test
    public void invalidJsonShouldThrowExceptions() throws GeneralSecurityException {
        final String decryptedJson = "{\"displayName}";
        when(encryptor.decrypt(ENCRYPTED_DATA)).thenReturn(decryptedJson);
        try {
            underTest.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage().trim(), is(equalTo("JSON deserialization error")));
        }
    }

    @Test(expected = GeneralSecurityException.class)
    public void failuretoDecryptShouldBlowUp() throws IOException, GeneralSecurityException {
        when(encryptor.decrypt(ENCRYPTED_DATA)).thenThrow(new GeneralSecurityException("Braaaap"));
        underTest.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA);
        fail();
    }


    private PlayerInformationHolder setupPlayerHolder() {
        PlayerInformationHolder tangoHolder = new PlayerInformationHolder();
        final PlayerProfile tangoProfile = new PlayerProfile();
        tangoProfile.setProviderName("TANGO");
        tangoProfile.setRpxProvider("TANGO");
        tangoProfile.setPartnerId(Partner.TANGO);
        tangoProfile.setExternalId("666");
        tangoProfile.setDisplayName("Jim");
        tangoProfile.setGuestStatus(GuestStatus.NON_GUEST);
        tangoHolder.setPlayerProfile(tangoProfile);
        tangoHolder.setAvatarUrl("http://your.mum/so/fat.gif");
        return tangoHolder;
    }
}
