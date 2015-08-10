package com.yazino.platform.event.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.Partner;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerProfileEvent implements PlatformEvent {

    private static final long serialVersionUID = -4165277347804649832L;
    @JsonProperty("id")
    private BigDecimal playerId;
    @JsonProperty("reg")
    private DateTime registrationTime;
    @JsonProperty("name")
    private String displayName;
    @JsonProperty("realName")
    private String realName;
    @JsonProperty("fName")
    private String firstName;
    @JsonProperty("lName")
    private String lastName;
    @JsonProperty("pic")
    private String pictureLocation;
    @JsonProperty("email")
    private String email;
    @JsonProperty("country")
    private String country;
    @JsonProperty("ext_id")
    private String externalId;
    @JsonProperty("ver_id")
    private String verificationIdentifier;
    @JsonProperty("prov")
    private String providerName;
    @JsonProperty("st")
    private PlayerProfileStatus status;
    @JsonProperty("ptnr")
    private Partner partnerId;

    @JsonProperty("dob")
    private DateTime dateOfBirth;
    @JsonProperty("gnd")
    private String gender;
    @JsonProperty("ref")
    private String inviteReferrerId;
    @JsonProperty("ip")
    private String ipAddress;
    @JsonProperty("new")
    private boolean newPlayer;
    @JsonProperty("gStatus")
    private String guestStatus;

    private PlayerProfileEvent() {
    }

    public PlayerProfileEvent(final BigDecimal playerId,
                              final DateTime registrationTime,
                              final String displayName,
                              final String realName,
                              final String firstName,
                              final String pictureLocation,
                              final String email,
                              final String country,
                              final String externalId,
                              final String verificationIdentifier,
                              final String providerName,
                              final PlayerProfileStatus status,
                              final Partner partnerId,
                              final DateTime dateOfBirth,
                              final String gender,
                              final String inviteReferrerId,
                              final String ipAddress,
                              final boolean newPlayer,
                              final String lastName,
                              final String guestStatus) {
        this.playerId = playerId;
        this.registrationTime = registrationTime;
        this.displayName = displayName;
        this.realName = realName;
        this.firstName = firstName;
        this.pictureLocation = pictureLocation;
        this.email = email;
        this.country = country;
        this.externalId = externalId;
        this.verificationIdentifier = verificationIdentifier;
        this.providerName = providerName;
        this.status = status;
        this.partnerId = partnerId;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.inviteReferrerId = inviteReferrerId;
        this.ipAddress = ipAddress;
        this.newPlayer = newPlayer;
        this.lastName = lastName;
        this.guestStatus = guestStatus;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.PLAYER_PROFILE;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public DateTime getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(final DateTime registrationTime) {
        this.registrationTime = registrationTime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(final String realName) {
        this.realName = realName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getPictureLocation() {
        return pictureLocation;
    }

    public void setPictureLocation(final String pictureLocation) {
        this.pictureLocation = pictureLocation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getVerificationIdentifier() {
        return verificationIdentifier;
    }

    public void setVerificationIdentifier(final String verificationIdentifier) {
        this.verificationIdentifier = verificationIdentifier;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(final String providerName) {
        this.providerName = providerName;
    }

    public PlayerProfileStatus getStatus() {
        return status;
    }

    public void setStatus(final PlayerProfileStatus status) {
        this.status = status;
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(final Partner partnerId) {
        this.partnerId = partnerId;
    }

    public DateTime getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final DateTime dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getInviteReferrerId() {
        return inviteReferrerId;
    }

    public void setInviteReferrerId(final String inviteReferrerId) {
        this.inviteReferrerId = inviteReferrerId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isNewPlayer() {
        return newPlayer;
    }

    public void setNewPlayer(final boolean newPlayer) {
        this.newPlayer = newPlayer;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getGuestStatus() {
        return guestStatus;
    }

    public void setGuestStatus(String guestStatus) {
        this.guestStatus = guestStatus;
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
        final PlayerProfileEvent rhs = (PlayerProfileEvent) obj;
        return new EqualsBuilder()
                .append(registrationTime, rhs.registrationTime)
                .append(displayName, rhs.displayName)
                .append(realName, rhs.realName)
                .append(firstName, rhs.firstName)
                .append(pictureLocation, rhs.pictureLocation)
                .append(email, rhs.email)
                .append(country, rhs.country)
                .append(externalId, rhs.externalId)
                .append(verificationIdentifier, rhs.verificationIdentifier)
                .append(providerName, rhs.providerName)
                .append(status, rhs.status)
                .append(partnerId, rhs.partnerId)
                .append(dateOfBirth, rhs.dateOfBirth)
                .append(gender, rhs.gender)
                .append(inviteReferrerId, rhs.inviteReferrerId)
                .append(newPlayer, rhs.newPlayer)
                .append(lastName, rhs.lastName)
                .append(guestStatus, rhs.guestStatus)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(registrationTime)
                .append(displayName)
                .append(realName)
                .append(firstName)
                .append(pictureLocation)
                .append(email)
                .append(country)
                .append(externalId)
                .append(verificationIdentifier)
                .append(providerName)
                .append(status)
                .append(partnerId)
                .append(dateOfBirth)
                .append(gender)
                .append(inviteReferrerId)
                .append(ipAddress)
                .append(newPlayer)
                .append(lastName)
                .append(guestStatus)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(registrationTime)
                .append(displayName)
                .append(realName)
                .append(firstName)
                .append(pictureLocation)
                .append(email)
                .append(country)
                .append(externalId)
                .append(verificationIdentifier)
                .append(providerName)
                .append(status)
                .append(partnerId)
                .append(dateOfBirth)
                .append(gender)
                .append(inviteReferrerId)
                .append(ipAddress)
                .append(newPlayer)
                .append(lastName)
                .append(guestStatus)
                .toString();
    }


}
