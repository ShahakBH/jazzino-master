package com.yazino.platform.player.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Service("authenticationService")
public class RemotingAuthenticationService implements AuthenticationService {

    private final YazinoAuthenticationService yazinoAuthenticationService;
    private final ExternalAuthenticationService externalAuthenticationService;

    @Autowired
    public RemotingAuthenticationService(final YazinoAuthenticationService yazinoAuthenticationService,
                                         final ExternalAuthenticationService externalAuthenticationService) {
        notNull(yazinoAuthenticationService, "yazinoAuthenticationService may not be null");
        notNull(externalAuthenticationService, "externalAuthenticationService may not be null");

        this.yazinoAuthenticationService = yazinoAuthenticationService;
        this.externalAuthenticationService = externalAuthenticationService;
    }

    @Override
    public PlayerProfileRegistrationResponse registerYazinoUser(final String email,
                                                                final String password,
                                                                final PlayerProfile profile,
                                                                final String remoteAddress,
                                                                final String referrer,
                                                                final Platform platform,
                                                                final String avatarUrl) {
        return registerYazinoUser(email, password, profile, remoteAddress, referrer, platform, avatarUrl, null);
    }

    @Override
    public PlayerProfileRegistrationResponse registerYazinoUser(final String email,
                                                                final String password,
                                                                final PlayerProfile profile,
                                                                final String remoteAddress,
                                                                final String referrer,
                                                                final Platform platform,
                                                                final String avatarURL,
                                                                final String gameType) {
        return yazinoAuthenticationService.register(email, password, profile, remoteAddress, referrer, platform, avatarURL, gameType);

    }

    @Override
    public PlayerProfileLoginResponse loginExternalUser(final String remoteAddress,
                                                        final Partner partnerId,
                                                        final PlayerInformationHolder provider,
                                                        final String referrer,
                                                        final Platform platform) {
        return loginExternalUser(remoteAddress, partnerId, provider, referrer, platform, null);
    }

    @Override
    public PlayerProfileLoginResponse loginExternalUser(final String remoteAddress,
                                                        final Partner partnerId,
                                                        final PlayerInformationHolder provider,
                                                        final String referrer,
                                                        final Platform platform,
                                                        final String gameType) {

        return externalAuthenticationService.login(remoteAddress, partnerId, provider, referrer, platform, gameType);
    }

    @Override
    public PlayerProfileLoginResponse loginYazinoUser(final String email,
                                                      final String password) {
        return yazinoAuthenticationService.login(email, password);
    }

    @Override
    public PlayerProfileAuthenticationResponse authenticateExternalUser(final String provider,
                                                                        final String externalId) {
        return externalAuthenticationService.authenticate(provider, externalId);
    }

    @Override
    public PlayerProfileAuthenticationResponse authenticateYazinoUser(final String emailAddress,
                                                                      final String password) {
        return yazinoAuthenticationService.authenticate(emailAddress, password);
    }

    @Override
    public void syncBuddies(BigDecimal playerId, String providerName, Set<String> friendExternalIds) {
        externalAuthenticationService.syncBuddies(playerId, providerName, friendExternalIds);
    }
}
