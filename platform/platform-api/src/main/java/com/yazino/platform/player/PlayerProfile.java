package com.yazino.platform.player;

import com.yazino.platform.Partner;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * The profile of a player.
 * <p/>
 * This contains auxiliary information which isn't directly relevant to the game host.
 */
public class PlayerProfile implements Serializable {
    private static final long serialVersionUID = 5374058939571545910L;

    private static final Partner DEFAULT_PARTNER_ID = Partner.YAZINO;

    private BigDecimal playerId;
    private String emailAddress;
    private String displayName;
    private String realName;
    private Gender gender;
    private String country;
    private String firstName;
    private String lastName;
    private DateTime dateOfBirth;
    private String referralIdentifier;
    private Partner partnerId;
    private String providerName;
    private String rpxProvider;
    private String externalId;
    private String verificationIdentifier;
    private PlayerProfileStatus status;
    private boolean syncProfile;
    private DateTime registrationTime;
    private Boolean optIn;
    private GuestStatus guestStatus;

    public PlayerProfile() {
    }

    public PlayerProfile(final String emailAddress,
                         final String displayName,
                         final String realName,
                         final Gender gender,
                         final String country,
                         final String firstName,
                         final String lastName,
                         final DateTime dateOfBirth,
                         final String referralIdentifier,
                         final String providerName,
                         final String rpxProvider,
                         final String externalId,
                         final boolean syncProfile) {
        this(null,
                emailAddress,
                displayName,
                realName,
                gender,
                country,
                firstName,
                lastName,
                dateOfBirth,
                referralIdentifier,
                providerName,
                rpxProvider,
                externalId,
                syncProfile
        );
    }

    public PlayerProfile(final BigDecimal playerId,
                         final String emailAddress,
                         final String displayName,
                         final String realName,
                         final Gender gender,
                         final String country,
                         final String firstName,
                         final String lastName,
                         final DateTime dateOfBirth,
                         final String referralIdentifier,
                         final String providerName,
                         final String rpxProvider,
                         final String externalId,
                         final boolean syncProfile) {
        this.playerId = playerId;
        this.emailAddress = emailAddress;
        this.displayName = displayName;
        this.realName = realName;
        this.gender = gender;
        this.country = country;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.referralIdentifier = referralIdentifier;
        this.providerName = providerName;
        this.rpxProvider = rpxProvider;
        this.externalId = externalId;
        this.syncProfile = syncProfile;
        this.optIn = false;
        this.status = PlayerProfileStatus.ACTIVE;
        this.partnerId = DEFAULT_PARTNER_ID;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRealName() {
        return realName;
    }

    public Gender getGender() {
        return gender;
    }

    public String getCountry() {
        return country;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public DateTime getDateOfBirth() {
        return dateOfBirth;
    }

    public String getReferralIdentifier() {
        return referralIdentifier;
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(final Partner partnerId) {
        this.partnerId = partnerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public void setRealName(final String realName) {
        this.realName = realName;
    }

    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setReferralIdentifier(final String referralIdentifier) {
        this.referralIdentifier = referralIdentifier;
    }

    public void setProviderName(final String providerName) {
        this.providerName = providerName;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public void setDateOfBirth(final DateTime dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public PlayerProfileStatus getStatus() {
        return status;
    }

    public void setStatus(final PlayerProfileStatus status) {
        notNull(status, "status may not be null");
        this.status = status;
    }

    public boolean isSyncProfile() {
        return syncProfile;
    }

    public void setSyncProfile(final boolean syncProfile) {
        this.syncProfile = syncProfile;
    }

    public String getRpxProvider() {
        return rpxProvider;
    }

    public void setRpxProvider(final String rpxProvider) {
        this.rpxProvider = rpxProvider;
    }

    public String getVerificationIdentifier() {
        return verificationIdentifier;
    }

    public void setVerificationIdentifier(final String verificationIdentifier) {
        this.verificationIdentifier = verificationIdentifier;
    }

    public DateTime getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(DateTime registrationTime) {
        this.registrationTime = registrationTime;
    }

    public GuestStatus getGuestStatus() {
        return guestStatus;
    }

    public void setGuestStatus(GuestStatus guestStatus) {
        notNull(guestStatus, "guest-status may not be null");
        this.guestStatus = guestStatus;
    }

    public Boolean getOptIn() {
        return optIn;
    }

    public void setOptIn(final Boolean optIn) {
        this.optIn = optIn;
    }

    public static PlayerProfileBuilder withPlayerId(final BigDecimal playerId) {
        return new PlayerProfileBuilder(playerId);
    }

    public static PlayerProfileBuilder copy(final PlayerProfile playerProfile) {
        notNull(playerProfile, "playerProfile may not be null");
        return new PlayerProfileBuilder(playerProfile);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final PlayerProfile rhs = (PlayerProfile) obj;
        return new EqualsBuilder()
                .append(emailAddress, rhs.emailAddress)
                .append(displayName, rhs.displayName)
                .append(realName, rhs.realName)
                .append(gender, rhs.gender)
                .append(country, rhs.country)
                .append(firstName, rhs.firstName)
                .append(lastName, rhs.lastName)
                .append(dateOfBirth, rhs.dateOfBirth)
                .append(referralIdentifier, rhs.referralIdentifier)
                .append(partnerId, rhs.partnerId)
                .append(providerName, rhs.providerName)
                .append(rpxProvider, rhs.rpxProvider)
                .append(externalId, rhs.externalId)
                .append(verificationIdentifier, rhs.verificationIdentifier)
                .append(status, rhs.status)
                .append(syncProfile, rhs.syncProfile)
                .append(registrationTime, rhs.registrationTime)
                .append(optIn, rhs.optIn)
                .append(guestStatus, rhs.guestStatus)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(emailAddress)
                .append(displayName)
                .append(realName)
                .append(gender)
                .append(country)
                .append(firstName)
                .append(lastName)
                .append(dateOfBirth)
                .append(referralIdentifier)
                .append(partnerId)
                .append(providerName)
                .append(rpxProvider)
                .append(externalId)
                .append(verificationIdentifier)
                .append(status)
                .append(syncProfile)
                .append(registrationTime)
                .append(optIn)
                .append(guestStatus)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(emailAddress)
                .append(displayName)
                .append(realName)
                .append(gender)
                .append(country)
                .append(firstName)
                .append(lastName)
                .append(dateOfBirth)
                .append(referralIdentifier)
                .append(partnerId)
                .append(providerName)
                .append(rpxProvider)
                .append(externalId)
                .append(verificationIdentifier)
                .append(status)
                .append(syncProfile)
                .append(registrationTime)
                .append(optIn)
                .append(guestStatus)
                .toString();
    }

    public static final class PlayerProfileBuilder {

        private BigDecimal playerIdToUse;
        private String emailAddressToUse;
        private String displayNameToUse;
        private String realNameToUse;
        private Gender genderToUse;
        private String countryToUse;
        private String firstNameToUse;
        private String lastNameToUse;
        private DateTime dateOfBirthToUse;
        private String referralIdentifierToUse;
        private Partner partnerIdToUse= Partner.YAZINO;
        private String providerNameToUse;
        private String rpxProviderToUse;
        private String externalIdToUse;
        private String verificationIdentifierToUse;
        private PlayerProfileStatus statusToUse = PlayerProfileStatus.ACTIVE;
        private boolean syncProfileToUse;
        private DateTime registrationTimeToUse;
        private GuestStatus guestStatusToUse = GuestStatus.NON_GUEST;

        private PlayerProfileBuilder(final BigDecimal playerId) {
            this.playerIdToUse = playerId;
        }

        private PlayerProfileBuilder(final PlayerProfile playerProfile) {
            notNull(playerProfile, "playerProfile may not be null");

            this.playerIdToUse = playerProfile.getPlayerId();
            this.emailAddressToUse = playerProfile.getEmailAddress();
            this.displayNameToUse = playerProfile.getDisplayName();
            this.realNameToUse = playerProfile.getRealName();
            this.genderToUse = playerProfile.getGender();
            this.countryToUse = playerProfile.getCountry();
            this.firstNameToUse = playerProfile.getFirstName();
            this.lastNameToUse = playerProfile.getLastName();
            this.dateOfBirthToUse = playerProfile.getDateOfBirth();
            this.referralIdentifierToUse = playerProfile.getReferralIdentifier();
            this.partnerIdToUse = playerProfile.getPartnerId();
            this.providerNameToUse = playerProfile.getProviderName();
            this.rpxProviderToUse = playerProfile.getRpxProvider();
            this.externalIdToUse = playerProfile.getExternalId();
            this.verificationIdentifierToUse = playerProfile.getVerificationIdentifier();
            this.statusToUse = playerProfile.getStatus();
            this.syncProfileToUse = playerProfile.isSyncProfile();
            this.registrationTimeToUse = playerProfile.getRegistrationTime();
            this.guestStatusToUse = playerProfile.getGuestStatus();
            this.partnerIdToUse = playerProfile.getPartnerId();

            if (statusToUse == null) {
                statusToUse = PlayerProfileStatus.ACTIVE;
            }
            if (guestStatusToUse == null) {
                guestStatusToUse = playerProfile.getGuestStatus();
            }
            if (partnerIdToUse == null) {
                partnerIdToUse = DEFAULT_PARTNER_ID;
            }
        }

        public PlayerProfileBuilder withPlayerId(final BigDecimal playerId) {
            this.playerIdToUse = playerId;
            return this;
        }

        public PlayerProfileBuilder withEmailAddress(final String emailAddress) {
            this.emailAddressToUse = emailAddress;
            return this;
        }

        public PlayerProfileBuilder withDisplayName(final String displayName) {
            this.displayNameToUse = displayName;
            return this;
        }

        public PlayerProfileBuilder withRealName(final String realName) {
            this.realNameToUse = realName;
            return this;
        }

        public PlayerProfileBuilder withGender(final Gender gender) {
            this.genderToUse = gender;
            return this;
        }

        public PlayerProfileBuilder withCountry(final String country) {
            this.countryToUse = country;
            return this;
        }

        public PlayerProfileBuilder withFirstName(final String firstName) {
            this.firstNameToUse = firstName;
            return this;
        }

        public PlayerProfileBuilder withLastName(final String lastName) {
            this.lastNameToUse = lastName;
            return this;
        }

        public PlayerProfileBuilder withDateOfBirth(final DateTime dateOfBirth) {
            this.dateOfBirthToUse = dateOfBirth;
            return this;
        }

        public PlayerProfileBuilder withReferralIdentifier(final String referralIdentifier) {
            this.referralIdentifierToUse = referralIdentifier;
            return this;
        }

        public PlayerProfileBuilder withPartnerId(final Partner partnerId) {
            this.partnerIdToUse = partnerId;
            return this;
        }

        public PlayerProfileBuilder withProviderName(final String providerName) {
            this.providerNameToUse = providerName;
            return this;
        }

        public PlayerProfileBuilder withRpxProvider(final String rpxProvider) {
            this.rpxProviderToUse = rpxProvider;
            return this;
        }

        public PlayerProfileBuilder withExternalId(final String externalId) {
            this.externalIdToUse = externalId;
            return this;
        }

        public PlayerProfileBuilder withVerificationIdentifier(final String verificationIdentifier) {
            this.verificationIdentifierToUse = verificationIdentifier;
            return this;
        }

        public PlayerProfileBuilder withStatus(final PlayerProfileStatus status) {
            notNull(status, "status may not be null");
            this.statusToUse = status;
            return this;
        }

        public PlayerProfileBuilder withSyncProfile(final boolean syncProfile) {
            this.syncProfileToUse = syncProfile;
            return this;
        }

        public PlayerProfileBuilder withRegistrationTime(DateTime newRegistrationTime) {
            this.registrationTimeToUse = newRegistrationTime;
            return this;
        }

        public PlayerProfileBuilder withGuestStatus(GuestStatus guestStatus) {
            notNull(guestStatus, "guest-status may not be null");
            this.guestStatusToUse = guestStatus;
            return this;
        }

        public PlayerProfile asProfile() {
            final PlayerProfile userProfile = new PlayerProfile(
                    playerIdToUse,
                    emailAddressToUse,
                    displayNameToUse,
                    realNameToUse,
                    genderToUse,
                    countryToUse,
                    firstNameToUse,
                    lastNameToUse,
                    dateOfBirthToUse,
                    referralIdentifierToUse,
                    providerNameToUse,
                    rpxProviderToUse,
                    externalIdToUse,
                    syncProfileToUse
            );

            userProfile.setPartnerId(partnerIdToUse);
            userProfile.setVerificationIdentifier(verificationIdentifierToUse);
            userProfile.setStatus(statusToUse);
            userProfile.setRegistrationTime(registrationTimeToUse);
            userProfile.setGuestStatus(guestStatusToUse);

            return userProfile;
        }

    }
}
