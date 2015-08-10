package com.yazino.platform.player.service;

import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.reference.ReferenceService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

@Service("playerProfileFactory")
public class PlayerProfileFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerProfileFactory.class);

    private final PlayerProfileDao playerProfileDao;
    private final QueuePublishingService<PlayerProfileEvent> playerProfileEventService;
    private final PlayerService playerService;
    private final ReferenceService referenceService;
    private final PlayerCreditConfiguration playerCreditConfiguration;
    private final InvitationService invitationService;
    private final QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService;
    private final AsyncEmailService asyncEmailService;

    @Autowired
    public PlayerProfileFactory(final PlayerProfileDao playerProfileDao,
                                @Qualifier("playerProfileEventQueuePublishingService")
                                final QueuePublishingService<PlayerProfileEvent> playerProfileEventService,
                                final PlayerService playerService,
                                final ReferenceService referenceService,
                                final PlayerCreditConfiguration playerCreditConfiguration,
                                final InvitationService invitationService,
                                @Qualifier("playerReferrerEventQueuePublishingService")
                                final QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService,
                                final AsyncEmailService asyncEmailService) {

        notNull(playerProfileDao, "playerProfileDao may not be null");
        notNull(playerProfileEventService, "playerProfileEventService may not be null");
        notNull(playerService, "playerService may not be null");
        notNull(referenceService, "referenceService may not be null");
        notNull(playerCreditConfiguration, "playerCreditConfiguration may not be null");
        notNull(invitationService, "invitationService may not be null");
        notNull(asyncEmailService, "asyncEmailService may not be null");

        this.playerReferrerEventService = playerReferrerEventService;
        this.playerProfileDao = playerProfileDao;
        this.playerProfileEventService = playerProfileEventService;
        this.playerService = playerService;
        this.referenceService = referenceService;
        this.playerCreditConfiguration = playerCreditConfiguration;
        this.invitationService = invitationService;
        this.asyncEmailService = asyncEmailService;
    }

    public BasicProfileInformation createAndPersistPlayerAndSendPlayerRegistrationEvent(final PlayerProfile playerProfile,
                                                                                        final String remoteAddress,
                                                                                        final String referrer,
                                                                                        final Platform platform,
                                                                                        final String avatarUrl) {
        return createAndPersistPlayerAndSendPlayerRegistrationEvent(playerProfile, remoteAddress, referrer, platform, avatarUrl, null);
    }

    public BasicProfileInformation createAndPersistPlayerAndSendPlayerRegistrationEvent(final PlayerProfile playerProfile,
                                                                                        final String remoteAddress,
                                                                                        final String referrer,
                                                                                        final Platform platform,
                                                                                        final String avatarUrl,
                                                                                        final String gameType) {
        notNull(playerProfile, "playerProfile may not be null");

        final BasicProfileInformation player = playerService.createNewPlayer(
                playerProfile.getDisplayName(), avatarUrl, playerProfile.getGuestStatus(), paymentPreferencesFor(playerProfile), playerCreditConfiguration);

        final DateTime registrationTime = new DateTime();
        playerProfile.setRegistrationTime(registrationTime);
        playerProfile.setPlayerId(player.getPlayerId());
        playerProfileDao.save(playerProfile);

        if (playerProfile.getEmailAddress() != null) {
            asyncEmailService.verifyAddress(playerProfile.getEmailAddress());
        }

        notifyInvitationService(playerProfile, registrationTime);
        playerProfileEventService.send(theEventFor(playerProfile, remoteAddress, registrationTime, avatarUrl));
        playerReferrerEventService.send(new PlayerReferrerEvent(playerProfile.getPlayerId(), referrer, platform.toString(), gameType));

        return player;
    }

    private void notifyInvitationService(final PlayerProfile playerProfile, final DateTime registrationTime) {
        if (playerProfile.getProviderName().equalsIgnoreCase("FACEBOOK")) {
            invitationService.invitationAccepted(playerProfile.getExternalId(), InvitationSource.FACEBOOK,
                    registrationTime, playerProfile.getPlayerId());
        } else if (playerProfile.getProviderName().equalsIgnoreCase("YAZINO")) {
            invitationService.invitationAccepted(playerProfile.getEmailAddress(), InvitationSource.EMAIL,
                    registrationTime, playerProfile.getPlayerId());
        } else if (playerProfile.getProviderName().equalsIgnoreCase("TANGO")) {
            invitationService.invitationAccepted(playerProfile.getExternalId(), InvitationSource.TANGO,
                                                 registrationTime, playerProfile.getPlayerId());
        } else {
            LOG.warn("Unsupported provider \"{}\"", playerProfile.getProviderName());
        }
    }

    private PaymentPreferences paymentPreferencesFor(final PlayerProfile playerProfile) {
        return new PaymentPreferences(referenceService.getPreferredCurrency(
                playerProfile.getCountry()));
    }

    private PlayerProfileEvent theEventFor(final PlayerProfile profile,
                                           final String remoteAddress,
                                           final DateTime registrationTime,
                                           final String avatarUrl) {
        return new PlayerProfileEvent(profile.getPlayerId(),
                registrationTime,
                profile.getDisplayName(),
                profile.getRealName(),
                profile.getFirstName(),
                avatarUrl,
                profile.getEmailAddress(),
                profile.getCountry(),
                profile.getExternalId(),
                profile.getVerificationIdentifier(),
                profile.getProviderName(),
                profile.getStatus(),
                profile.getPartnerId(),
                profile.getDateOfBirth(),
                genderOf(profile),
                profile.getReferralIdentifier(),
                remoteAddress,
                true,
                profile.getLastName(),
                guestStatusOf(profile));
    }

    private String genderOf(final PlayerProfile profile) {
        final String gender;
        if (profile.getGender() == null) {
            gender = null;
        } else {
            gender = profile.getGender().getId();
        }
        return gender;
    }

    private String guestStatusOf(final PlayerProfile profile) {
        GuestStatus guestStatus = profile.getGuestStatus();
        if (guestStatus == null) {
            guestStatus = GuestStatus.NON_GUEST;
        }
        return guestStatus.getId();
    }

}
