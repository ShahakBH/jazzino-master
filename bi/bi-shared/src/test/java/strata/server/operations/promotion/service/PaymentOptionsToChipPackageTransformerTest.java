package strata.server.operations.promotion.service;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PaymentOptionBuilder;
import com.yazino.bi.payment.persistence.JDBCPaymentOptionDAO;
import com.yazino.platform.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.operations.promotion.model.ChipPackage;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaymentOptionsToChipPackageTransformerTest {
    public static final BigDecimal AMOUNT_REAL_MONEY_PER_PURCHASE = BigDecimal.TEN;
    public static final String CURRENCY_LABEL = "USD";
    public static final String REAL_MONEY_CURRENCY = "GBP";
    private PaymentOptionsToChipPackageTransformer underTest;
    @Mock
    private JDBCPaymentOptionDAO paymentOptionDao;


    @Before
    public void setUp() throws Exception {
        underTest = new PaymentOptionsToChipPackageTransformer(paymentOptionDao);
    }

    @Test
    public void shouldReturnDefaultPackagesForEveryPlatform() throws Exception {
        for(Platform platform : Platform.values())  {
            when(paymentOptionDao.findByPlatform(platform)).thenReturn(paymentOptionsFor(platform));
        }

        Map<Platform,List<ChipPackage>> actual = underTest.getDefaultPackages();

        for(Platform platform : Platform.values())  {
            ChipPackage expectedChipPackage = new ChipPackage();
            expectedChipPackage.setDefaultChips(BigDecimal.valueOf(platform.ordinal() + 1000));
            assertThat(actual.get(platform).get(0), is(equalTo(expectedChipPackage)));

        }

    }

    private HashSet<PaymentOption> paymentOptionsFor(Platform platform) {
        int numChips = platform.ordinal() + 1000;
        PaymentOption expectedPaymentOption = new PaymentOptionBuilder()
                .setId("bollocks1")
                .setAmountRealMoneyPerPurchase(AMOUNT_REAL_MONEY_PER_PURCHASE)
                .setCurrencyLabel(CURRENCY_LABEL)
                .setNumChipsPerPurchase(BigDecimal.valueOf(numChips))
                .setRealMoneyCurrency(REAL_MONEY_CURRENCY).createPaymentOption();
        return newHashSet(expectedPaymentOption);
    }
}
