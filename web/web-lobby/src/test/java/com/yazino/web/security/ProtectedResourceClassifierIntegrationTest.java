package com.yazino.web.security;

import com.yazino.game.api.GameType;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.data.GameTypeRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class ProtectedResourceClassifierIntegrationTest {

    @Resource(name = "publicDomain")
    private Domain publicDomain;
    private Domain gamesDomain;

    @Mock
    private GameTypeRepository gameTypeRepository;
    private HashMap<String, GameTypeInformation> gameTypeInformationHashMap;
    private List<String> gameIds;
    private List<String> gameNames;
    private List<Set<String>> pseudonyms;

    private ProtectedResourceClassifier underTest;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        gamesDomain = new GamesDomain(gameTypeRepository);

        gameTypeInformationHashMap = new HashMap<String, GameTypeInformation>();
        gameIds = Arrays.asList("HIGH_STAKES", "BLACKJACK", "TEXAS_HOLDEM", "ROULETTE", "SLOTS");
        gameNames = Arrays.asList("highStakes", "blackjack", "texasHoldem", "roulette", "wheelDeal");
        pseudonyms = Arrays.asList((Set<String>)
                newHashSet("highStakes", "HighStakes", "highstakes", "HIGH_STAKES", "hs"),
                new HashSet<String>(),
                newHashSet("texasHoldem", "TexasHolden", "texasholdem", "TEXAS_HOLDEM", "th", "texas", "holdem", "poker", "Poker"),
                newHashSet("roulette", "Roulette", "ROULETTE", "rl"),
                newHashSet("slots", "Slots", "SLOTS", "sl", "wheeldeal", "wheelDeal", "WheelDeal", "WHEEL_DEAL"));

        gameTypeInformationHashMap.put(gameIds.get(0), createGameTypeInformation(0));
        gameTypeInformationHashMap.put(gameIds.get(1), createGameTypeInformation(1));
        gameTypeInformationHashMap.put(gameIds.get(2), createGameTypeInformation(2));
        gameTypeInformationHashMap.put(gameIds.get(3), createGameTypeInformation(3));
        gameTypeInformationHashMap.put(gameIds.get(4), createGameTypeInformation(4));

        underTest = new ProtectedResourceClassifier(newHashSet(publicDomain, gamesDomain));
        when(gameTypeRepository.getGameTypes()).thenReturn(gameTypeInformationHashMap);

    }

    @Test
    public void ShouldReturnFalseForAllPublicUrls() {
        assertFalse(underTest.requiresAuthorisation("/facebookLogin/"));
        assertFalse(underTest.requiresAuthorisation("/public/facebookLogin/"));
        assertFalse(underTest.requiresAuthorisation("/"));
        assertFalse(underTest.requiresAuthorisation("/public/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/publicCommand/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/lobbyCommand/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/command/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/error/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/rules/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/legal/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/friends"));
        assertFalse(underTest.requiresAuthorisation("/index"));
        assertFalse(underTest.requiresAuthorisation("/public"));
        assertFalse(underTest.requiresAuthorisation("/login"));
        assertFalse(underTest.requiresAuthorisation("/login/FACEBOOK"));
        assertFalse(underTest.requiresAuthorisation("/fb/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/facebookLogin/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/facebookOAuthLogin/anydir"));
        assertFalse(underTest.requiresAuthorisation("/connectLogin"));
        assertFalse(underTest.requiresAuthorisation("/connectLogin"));
        assertFalse(underTest.requiresAuthorisation("/registration"));
        assertFalse(underTest.requiresAuthorisation("/resetPassword"));
        assertFalse(underTest.requiresAuthorisation("/referral"));
        assertFalse(underTest.requiresAuthorisation("/maintenance"));
        assertFalse(underTest.requiresAuthorisation("/maintenance/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/mytable"));
        assertFalse(underTest.requiresAuthorisation("/contactus"));
        assertFalse(underTest.requiresAuthorisation("/aboutus"));
        assertFalse(underTest.requiresAuthorisation("/sitemap"));
        assertFalse(underTest.requiresAuthorisation("/games"));
        assertFalse(underTest.requiresAuthorisation("/tournaments"));
        assertFalse(underTest.requiresAuthorisation("/tournaments/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/lobby/games"));
        assertFalse(underTest.requiresAuthorisation("/game"));
        assertFalse(underTest.requiresAuthorisation("/lobby/game"));
        assertFalse(underTest.requiresAuthorisation("/blackjack"));
        assertFalse(underTest.requiresAuthorisation("/blackJack"));
        assertFalse(underTest.requiresAuthorisation("/Blackjack"));
        assertFalse(underTest.requiresAuthorisation("/BlackJack"));
        assertFalse(underTest.requiresAuthorisation("/BLACKJACK"));
        assertFalse(underTest.requiresAuthorisation("/roulette"));
        assertFalse(underTest.requiresAuthorisation("/Roulette"));
        assertFalse(underTest.requiresAuthorisation("/ROULETTE"));
        assertFalse(underTest.requiresAuthorisation("/texasholdem"));
        assertFalse(underTest.requiresAuthorisation("/texasHoldem"));
        assertFalse(underTest.requiresAuthorisation("/TexasHoldem"));
        assertFalse(underTest.requiresAuthorisation("/TEXAS_HOLDEM"));
        assertFalse(underTest.requiresAuthorisation("/poker"));
        assertFalse(underTest.requiresAuthorisation("/Poker"));
        assertFalse(underTest.requiresAuthorisation("/wheeldeal"));
        assertFalse(underTest.requiresAuthorisation("/wheelDeal"));
        assertFalse(underTest.requiresAuthorisation("/WheelDeal"));
        assertFalse(underTest.requiresAuthorisation("/WHEEL_DEAL"));
        assertFalse(underTest.requiresAuthorisation("/slots"));
        assertFalse(underTest.requiresAuthorisation("/Slots"));
        assertFalse(underTest.requiresAuthorisation("/SLOTS"));
        assertFalse(underTest.requiresAuthorisation("/highstakes"));
        assertFalse(underTest.requiresAuthorisation("/highStakes"));
        assertFalse(underTest.requiresAuthorisation("/HighStakes"));
        assertFalse(underTest.requiresAuthorisation("/HIGH_STAKES"));
        assertFalse(underTest.requiresAuthorisation("/buyChips/inGameMessage"));
        assertFalse(underTest.requiresAuthorisation("/lobbyInformation"));
        assertFalse(underTest.requiresAuthorisation("/noCookies"));
        assertFalse(underTest.requiresAuthorisation("/player/balance"));
        assertFalse(underTest.requiresAuthorisation("/social/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/not-allowed/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/jobs"));
        assertFalse(underTest.requiresAuthorisation("/management"));
        assertFalse(underTest.requiresAuthorisation("/management/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/lobby/checkNewMessages"));
        assertFalse(underTest.requiresAuthorisation("/messages/check"));
        assertFalse(underTest.requiresAuthorisation("/verify/anyfile/anyfile/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/support/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/strata.server.lobby.support/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/channel.html"));
        assertFalse(underTest.requiresAuthorisation("/command/version"));
        assertFalse(underTest.requiresAuthorisation("/payment/trialpay/callback"));
        assertFalse(underTest.requiresAuthorisation("/payment/radium/callback"));
        assertFalse(underTest.requiresAuthorisation("/payment/facebook/callback"));
        assertFalse(underTest.requiresAuthorisation("/payment/itunes/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/api/anyfile/promotion/anyfile/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/fbog/anydir/anyfile"));
        assertFalse(underTest.requiresAuthorisation("/mobile/tapjoy/anydir/anyfile"));
    }

    private GameTypeInformation createGameTypeInformation(final int index) {
        return new GameTypeInformation(new GameType(gameIds.get(index), gameNames.get(index), pseudonyms.get(index)), true);
    }
}
