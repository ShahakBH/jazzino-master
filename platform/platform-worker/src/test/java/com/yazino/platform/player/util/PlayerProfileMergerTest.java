package com.yazino.platform.player.util;

import com.yazino.platform.player.Gender;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.yazino.platform.Partner.YAZINO;

public class PlayerProfileMergerTest {

    private PlayerProfile baseProfile;
    private PlayerProfile mergeeProfile;

    @Before
    public void setup() {
        baseProfile = new PlayerProfile();
        mergeeProfile = new PlayerProfile();

        baseProfile.setOptIn(false);
        baseProfile.setStatus(PlayerProfileStatus.ACTIVE);
        baseProfile.setGuestStatus(GuestStatus.CONVERTED);
        baseProfile.setPartnerId(YAZINO);
        mergeeProfile.setOptIn(false);
        mergeeProfile.setPartnerId(YAZINO);
        mergeeProfile.setStatus(PlayerProfileStatus.ACTIVE);
        mergeeProfile.setGuestStatus(GuestStatus.CONVERTED);
    }

    @Test
    public void doesntMerge_whenIncomingProfileEmailAddressIsNull() throws Exception {
        baseProfile.setEmailAddress("foo@bar.com");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setEmailAddress(null);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasEmailAddressChanged());
    }

    @Test
    public void doesntMerge_whenIncomingProfileEmailAddressIsEmpty() throws Exception {
        baseProfile.setEmailAddress("foo@bar.com");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setEmailAddress("");
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasEmailAddressChanged());
    }

    @Test
    public void merges_whenIncomingProfileEmailAddressIsValid() throws Exception {
        baseProfile.setEmailAddress("foo@bar.com");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setEmailAddress("bar@foo.com");
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals("bar@foo.com", merged.getEmailAddress());
        Assert.assertTrue(merger.hasEmailAddressChanged());
    }

    @Test
    public void doesntMerge_whenIncomingProfileDisplayNameIsNull() throws Exception {
        baseProfile.setDisplayName("Test");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setDisplayName(null);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasDisplayNameChanged());
    }

    @Test
    public void doesntMerge_whenIncomingProfileDisplayNameIsEmpty() throws Exception {
        baseProfile.setDisplayName("Test");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setDisplayName("");
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasDisplayNameChanged());
    }

    @Test
    public void merges_whenIncomingProfileDisplayNameIsValid() throws Exception {
        baseProfile.setDisplayName("Test");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setDisplayName("Foo");
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals("Foo", merged.getDisplayName());
        Assert.assertTrue(merger.hasDisplayNameChanged());
    }

    @Test
    public void doesntMerge_whenIncomingProfileRealNameIsNull() throws Exception {
        baseProfile.setRealName("test Name");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setRealName(null);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasRealNameChanged());
    }

    @Test
    public void doesntMerge_whenIncomingProfileRealNameIsEmpty() throws Exception {
        baseProfile.setRealName("test Name");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setRealName("");
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasRealNameChanged());
    }

    @Test
    public void merges_whenIncomingProfileRealNameIsValid() throws Exception {
        baseProfile.setRealName("test Name");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setRealName("BigBadTest");
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals("BigBadTest", merged.getRealName());
        Assert.assertTrue(merger.hasRealNameChanged());
    }

    @Test
    public void doesntMerge_whenIncomingProfileGenderIsNull() throws Exception {
        baseProfile.setGender(Gender.MALE);
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setGender(null);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasGenderChanged());
    }

    @Test
    public void merges_whenIncomingProfileGenderIsValid() throws Exception {
        baseProfile.setGender(Gender.MALE);
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setGender(Gender.FEMALE);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(Gender.FEMALE, merged.getGender());
        Assert.assertTrue(merger.hasGenderChanged());
    }

    @Test
    public void merges_whenIncomingProfileGuestStatusIsValid() throws Exception {
        baseProfile.setGuestStatus(GuestStatus.GUEST);
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setGuestStatus(GuestStatus.CONVERTED);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(GuestStatus.CONVERTED, merged.getGuestStatus());
        Assert.assertTrue(merger.hasGuestStatusChanged());
    }

    @Test
    public void doesntMerge_whenIncomingProfileCountryIsNull() throws Exception {
        baseProfile.setCountry("UK");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setCountry(null);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasCountryChanged());
    }

    @Test
    public void doesntMerge_whenIncomingProfileCountryIsEmpty() throws Exception {
        baseProfile.setCountry("UK");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setCountry("");
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals(baseProfile, merged);
        Assert.assertFalse(merger.hasCountryChanged());
    }

    @Test
    public void merges_whenIncomingProfileCountryIsValid() throws Exception {
        baseProfile.setCountry("UK");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        mergeeProfile.setCountry("USA");
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals("USA", merged.getCountry());
        Assert.assertTrue(merger.hasCountryChanged());
    }

    @Test
    public void merges_referralId() throws Exception {
        baseProfile.setReferralIdentifier("Test");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals("Test", merged.getReferralIdentifier());
    }

    @Test
    public void merges_provider() throws Exception {
        baseProfile.setProviderName("Test");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals("Test", merged.getProviderName());
    }

    @Test
    public void merges_externalId() throws Exception {
        baseProfile.setExternalId("Test");
        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        PlayerProfile merged = merger.merge(mergeeProfile);
        Assert.assertEquals("Test", merged.getExternalId());
    }

    @Test
    public void allFieldsChanged() throws Exception {
        baseProfile.setEmailAddress("foo@bar.com");
        baseProfile.setDisplayName("Test");
        baseProfile.setRealName("test Name");
        baseProfile.setCountry("UK");
        baseProfile.setGender(Gender.MALE);

        mergeeProfile.setEmailAddress("bar@foo.com");
        mergeeProfile.setDisplayName("Test123");
        mergeeProfile.setRealName("test Name2");
        mergeeProfile.setCountry("USA");
        mergeeProfile.setGender(Gender.FEMALE);

        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        merger.merge(mergeeProfile);
        Assert.assertTrue(merger.haveFieldsChanged());
    }

    @Test
    public void oneOfMergedFieldsHaveChanged() throws Exception {
        baseProfile.setEmailAddress("foo@bar.com");
        baseProfile.setDisplayName("Test");
        baseProfile.setRealName("test Name");
        baseProfile.setCountry("UK");
        baseProfile.setGender(Gender.MALE);

        mergeeProfile.setCountry("US");

        PlayerProfileMerger merger = new PlayerProfileMerger(baseProfile);
        merger.merge(mergeeProfile);
        Assert.assertTrue(merger.haveFieldsChanged());
    }

}
