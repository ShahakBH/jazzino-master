package com.yazino.web.controller;

import com.yazino.platform.Partner;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.web.domain.world.TangoLoginJson;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.SymmetricEncryptor;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class TangoPlayerInformationProvider {

    private final SymmetricEncryptor encryptor;
    private final JsonHelper jsonHelper;

    private static final Partner TANGO_PARTNER_ID = Partner.TANGO;

    @Autowired
    public TangoPlayerInformationProvider(final SymmetricEncryptor encryptor, final JsonHelper jsonHelper) {
        Validate.notNull(encryptor);
        Validate.notNull(jsonHelper);
        this.encryptor = encryptor;
        this.jsonHelper = jsonHelper;
    }

//    @Deprecated
//    public PlayerInformationHolder getUserInformationHolder(final String accessToken) {
//        final PlayerInformationHolder holder = new PlayerInformationHolder();
//        final PlayerProfile playerProfile = new PlayerProfile();
//        holder.setPlayerProfile(playerProfile);
//        playerProfile.setDisplayName("");
//        playerProfile.setProviderName("TANGO");
//        playerProfile.setPartnerId("TANGO");
//        playerProfile.setExternalId(accessToken);//fake it til you make it!
//
//        return holder;
//    }

    public PlayerInformationHolder getPlayerInformationFromEncryptedData(final String encryptedData)
            throws GeneralSecurityException {
        Validate.notNull(encryptedData);
        final String decrypted = encryptor.decrypt(encryptedData);
        final TangoLoginJson loginJson = jsonHelper.deserialize(TangoLoginJson.class, decrypted);
        final PlayerInformationHolder holder = new PlayerInformationHolder();
        final PlayerProfile playerProfile = new PlayerProfile();
        assertLoginDataValid(loginJson);
        playerProfile.setPartnerId(TANGO_PARTNER_ID);
        playerProfile.setProviderName(TANGO_PARTNER_ID.name());
        playerProfile.setRpxProvider(TANGO_PARTNER_ID.name());
        playerProfile.setGuestStatus(GuestStatus.NON_GUEST);
        playerProfile.setDisplayName(loginJson.getDisplayName());
        playerProfile.setExternalId(loginJson.getAccountId());
        holder.setAvatarUrl(loginJson.getAvatarUrl());
        holder.setPlayerProfile(playerProfile);
        return holder;
    }

    private void assertLoginDataValid(final TangoLoginJson loginJson) throws RuntimeException {
        if (isBlank(loginJson.getDisplayName())) {
            throw new RuntimeException("Missing DisplayName");
        }
        if (isBlank(loginJson.getAccountId())) {
            throw new RuntimeException("Missing AccountId");
        }
    }

}
