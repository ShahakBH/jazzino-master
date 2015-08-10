package com.yazino.platform.player.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.*;

import java.math.BigDecimal;
import java.util.Set;

public interface AuthenticationService {

    PlayerProfileRegistrationResponse registerYazinoUser(String email,
                                                         String password,
                                                         PlayerProfile profile,
                                                         String remoteAddress,
                                                         String referrer,
                                                         Platform platform,
                                                         String avatarURL);

    PlayerProfileRegistrationResponse registerYazinoUser(String email,
                                                         String password,
                                                         PlayerProfile profile,
                                                         String remoteAddress,
                                                         String referrer,
                                                         Platform platform,
                                                         String avatarURL,
                                                         String gameType);

    PlayerProfileLoginResponse loginExternalUser(String remoteAddress,
                                                 Partner partnerId,
                                                 PlayerInformationHolder provider,
                                                 String referrer,
                                                 Platform platform);

    PlayerProfileLoginResponse loginExternalUser(String remoteAddress,
                                                 Partner partnerId,
                                                 PlayerInformationHolder provider,
                                                 String referrer,
                                                 Platform platform,
                                                 String gameType);

    PlayerProfileLoginResponse loginYazinoUser(String email,
                                               String password);

    PlayerProfileAuthenticationResponse authenticateExternalUser(String provider,
                                                                 String externalId);

    PlayerProfileAuthenticationResponse authenticateYazinoUser(String emailAddress,
                                                               String password);

    void syncBuddies(BigDecimal playerId, String providerName, Set<String> friendExternalIds);

}
