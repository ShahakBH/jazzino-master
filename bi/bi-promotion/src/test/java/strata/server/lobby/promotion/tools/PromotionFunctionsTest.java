package strata.server.lobby.promotion.tools;

import org.junit.Before;
import org.junit.Test;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.promotion.domain.ExternalCredentials;
import strata.server.lobby.promotion.service.PromotionControlGroupServiceImpl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class PromotionFunctionsTest {
    private PromotionControlGroupServiceImpl.LegacyPromotionFunctions underTest;

    @Before
    public void init() {
        underTest = new PromotionControlGroupServiceImpl.LegacyPromotionFunctions();
    }

    @Test
    public void whenControlGroupPercentageIsZeroAllPlayersAreActiveIrrespectiveOfSeed() {
        Random random = new Random();

        for (long playerId = 1; playerId < Promotion.MAX_SEED_VALUE * 1000; playerId++) {
            int seed = random.nextInt(Promotion.MAX_SEED_VALUE + 1);
            final boolean controlGroupMember = underTest.isControlGroupMember(BigInteger.valueOf(playerId), seed, 0);
            assertFalse(controlGroupMember);
        }
    }

    @Test
    public void whenControlGroupPercentageIs100AllPlayersAreControlGroupMembersIrrespectiveOfSeed() {
        Random random = new Random();

        for (long playerId = 1; playerId < Promotion.MAX_SEED_VALUE * 1000; playerId++) {
            int seed = random.nextInt(Promotion.MAX_SEED_VALUE + 1);
            final boolean controlGroupMember = underTest.isControlGroupMember(BigInteger.valueOf(playerId), seed, 100);
            assertTrue(controlGroupMember);
        }
    }

    @Test
    public void whenPlayerIdPlusSeedModLessThanSeedThenPlayerIsControlGroupMember() {
        int controlGroupPercentage = 11;
        int seed = 59;
        BigInteger playerId = BigInteger.valueOf(51);
        assertTrue(underTest.isControlGroupMember(playerId, seed, controlGroupPercentage));

    }

    @Test
    public void whenPlayerIdMod100GreaterOrEqualToSeedThenPlayerIsNotControlGroupMember() {
        int controlGroupPercentage = 11;
        int seed = 59;
        BigInteger playerId = BigInteger.valueOf(52);
        assertFalse(underTest.isControlGroupMember(playerId, seed, controlGroupPercentage));
    }

    @Test
    public void whenControlGroupPercentageIs15Then15PercentOfPlayersAreInControlGroup() {
        int numberOfPlayers = 1000;
        int controlGroupPercentage = 15;
        int expectedNumberOfControlGroupPlayers = 150;
        for (int seed = 0; seed <= 99; seed++) {
            int controlGroupCount = 0;
            for (int playerId = 1; playerId <= numberOfPlayers; playerId++) {
                if (underTest.isControlGroupMember(BigInteger.valueOf(playerId), seed, controlGroupPercentage)) {
                    controlGroupCount++;
                }
            }
            assertThat(controlGroupCount, is(expectedNumberOfControlGroupPlayers));
        }
    }

    @Test
    public void playerShouldBeDistributedCorrectly() {
        // GIVEN control group % 13 and player id 11
        BigInteger playerId = BigInteger.valueOf(11);
        int controlGroupPercentage = 13;
        List<Integer> expectedSeedControlGroupMembership = Arrays.asList(1, 0, 99, 98, 97, 96, 95, 94, 93, 92, 91, 90, 89);

        // THEN over seed range player should be a control group member when seed lies
        // in range 99, 98, 97..89 0, 1 inclusive
        List<Integer> actualSeedControlGroupMembership = new ArrayList<Integer>();
        for (int seed = 0; seed <= 99; seed++) {
            if (underTest.isControlGroupMember(playerId, seed, controlGroupPercentage)) {
                actualSeedControlGroupMembership.add(seed);
            }
        }
        assertThat(actualSeedControlGroupMembership.size(), is(13));
        assertTrue(actualSeedControlGroupMembership.containsAll(expectedSeedControlGroupMembership));
    }

    @Test
    public void whenUsingExternalCredentialsIsControlGroupMemberShouldReturnTrue() {
        // Given the following credentials, seed and cg percentage
        ExternalCredentials externalCredentials = new ExternalCredentials("facebook", "103060982");
        // int value of 4075660507  (after converting low order 4 bytes of hash to unsigned int)
        int seed = 10;
        int cgPercentage = 18;

        // when checking whether a player is a target
        final boolean isControl = underTest.isControlGroupMember(externalCredentials, seed, cgPercentage);

        // then player should be a control since 4075660507 + 10 % 100 = 17 and 17 - 18 is -ve
        assertTrue(isControl);
    }

    @Test
    public void whenUsingExternalCredentialsIsControlGroupMemberShouldReturnFalse() {
        // Given the following credentials, seed and cg percentage
        ExternalCredentials externalCredentials = new ExternalCredentials("facebook", "103060982");
        // int value of 4075660507  (after converting low order 4 bytes of hash to unsigned int)
        int seed = 10;
        int cgPercentage = 17;

        // when checking whether a player is a target
        final boolean isControl = underTest.isControlGroupMember(externalCredentials, seed, cgPercentage);

        // then player should not be a control since 4075660507 + 10 % 100 = 17 and 17 - 17 is >= 0
        assertFalse(isControl);
    }

    @Test
    public void whenUsingExternalCredentialsAndControlGroupPercentageIsZeroAllPlayersAreActiveIrrespectiveOfSeed() {
        Random random = new Random();

        for (long externalId = 1; externalId < Promotion.MAX_SEED_VALUE * 1000; externalId++) {
            int seed = random.nextInt(Promotion.MAX_SEED_VALUE + 1);
            ExternalCredentials externalCredentials = new ExternalCredentials("anything really", "" + externalId);
            final boolean controlGroupMember = underTest.isControlGroupMember(externalCredentials, seed, 0);
            assertFalse(controlGroupMember);
        }
    }

    @Test
    public void whenUsingExternalCredentialsAndControlGroupPercentageIs100AllPlayersAreControlGroupMembersIrrespectiveOfSeed() {
        Random random = new Random();

        for (long externalId = 1; externalId < Promotion.MAX_SEED_VALUE * 1000; externalId++) {
            int seed = random.nextInt(Promotion.MAX_SEED_VALUE + 1);
            ExternalCredentials externalCredentials = new ExternalCredentials("something, anything", "" + externalId);
            final boolean controlGroupMember = underTest.isControlGroupMember(externalCredentials, seed, 100);
            assertTrue(controlGroupMember);
        }
    }

    @Test
    public void whenTheExternalCredentialsAreNullTheControlGroupMembershipShouldBeFalse() {
        final boolean controlGroupMember = underTest.isControlGroupMember((ExternalCredentials) null, 10, 100);

        assertFalse(controlGroupMember);
    }

    @Test
    public void whenUsingExternalCredentialsPlayerShouldBeDistributedCorrectly() {
        // Given the following credentials and cg percentage
        ExternalCredentials externalCredentials = new ExternalCredentials("facebook", "103060982");
        // int value of 4075660507  (after converting low order 4 bytes of hash to unsigned int)
        int cgPercentage = 7;

        // THEN over seed range player should be a control group member when seed lies
        // in range 93 to 99 inclusive
        // since to be a control 4075660507 + seed % 100 - 7 must be -ve
        // so possible seed values are
        List<Integer> expectedSeedControlGroupMembership = Arrays.asList(93, 94, 95, 96, 97, 98, 99);

        List<Integer> actualSeedControlGroupMembership = new ArrayList<Integer>();
        for (int seed = 0; seed <= 99; seed++) {
            if (underTest.isControlGroupMember(externalCredentials, seed, cgPercentage)) {
                actualSeedControlGroupMembership.add(seed);
            }
        }
        assertThat(actualSeedControlGroupMembership.size(), is(cgPercentage));
        assertTrue(actualSeedControlGroupMembership.containsAll(expectedSeedControlGroupMembership));
    }
}
