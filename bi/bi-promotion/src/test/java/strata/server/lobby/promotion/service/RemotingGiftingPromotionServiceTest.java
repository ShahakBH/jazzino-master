package strata.server.lobby.promotion.service;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.gifting.AppToUserGift;
import com.yazino.promotions.PromotionDao;
import com.yazino.promotions.PromotionPlayerReward;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.promotion.GiftingPromotion;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RemotingGiftingPromotionServiceTest {
    public static final String GIFT_TITLE = "GIFT TITLE YEAH?";
    public static final String GIFT_DESCRIPTION = "GIFT DESCRIPTION YEAH?";
    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    public static final BigDecimal SESSION_ID = BigDecimal.TEN;

    @Mock
    private PromotionDao promotionDao;
    private RemotingGiftingPromotionService remotingGiftingPromotionService;
    @Mock
    private PlayerService playerService;

    @Before
    public void setUp() throws Exception {
        remotingGiftingPromotionService = new RemotingGiftingPromotionService(promotionDao, playerService);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());
    }

    @Test
    public void getGiftingPromotionsShouldConvertPromotionsToGifts() {

        final List<Promotion> promosForNumberOne = newArrayList();
        final GiftingPromotion promo = new GiftingPromotion(now().minusHours(1), now().plusHours(23), 2000L, true, "testizles", GIFT_TITLE, GIFT_DESCRIPTION, "SLORTS");
        promo.setId(123L);
        promosForNumberOne.add(promo);
        when(promotionDao.findPromotionsFor(BigDecimal.ONE, PromotionType.GIFTING, null, now()))
                .thenReturn(promosForNumberOne);

        final List<AppToUserGift> gifts = remotingGiftingPromotionService.getGiftingPromotions(BigDecimal.ONE);

        assertThat(gifts.size(), is(1));
        final AppToUserGift gift = gifts.iterator().next();
        assertThat(gift.getAmount(), is(2000L));
        assertThat(gift.getDescription(), is(GIFT_DESCRIPTION));
        assertThat(gift.getTitle(), is(GIFT_TITLE));
        assertThat(gift.getTitle(), is(GIFT_TITLE));
        assertThat(gift.getGameType(), is("SLORTS"));
        assertThat(gift.getExpiry(), is(now().plusHours(23)));
        assertThat(gift.getPromoId(), is(123L));
    }

    @Test
    public void logPlayerRewardShouldGiveChipsAndLogReward() throws WalletServiceException {
        final GiftingPromotion promo = new GiftingPromotion(now().minusHours(1), now().plusHours(23), 2000L, true, "testizles", GIFT_TITLE, GIFT_DESCRIPTION, "SLORTS");
        promo.setId(123l);
        final List<Promotion> promos = new ArrayList<Promotion>();
        promos.add(promo);
        when(promotionDao.findPromotionsFor(PLAYER_ID, PromotionType.GIFTING, null, now())).thenReturn(promos);

        remotingGiftingPromotionService.logPlayerReward(PLAYER_ID, 123L, SESSION_ID);

        final PromotionPlayerReward promotionPlayerReward = new PromotionPlayerReward(123L, PLAYER_ID, false, now(), "reward=2000");
        verify(promotionDao).addLastReward(promotionPlayerReward);
        verify(playerService).postTransaction(PLAYER_ID, SESSION_ID, BigDecimal.valueOf(2000), "AppToUser Gift", "reward=2000");

    }
}
