package com.yazino.web.payment;

import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import com.yazino.web.service.FacebookCurrencyService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ProductPreferredCurrencyServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final String GAME_TYPE = "SLOTS";

    private ProductPreferredCurrencyService underTest;

    @Mock
    private FacebookCurrencyService facebookCurrencyService;
    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new ProductPreferredCurrencyService(facebookCurrencyService);
    }

    @Test
    public void shouldReturnUSDWhenPlatformIsNotFACEBOOK_CANVAS() {
        for (Platform platform: Platform.values()) {
            if (platform == Platform.FACEBOOK_CANVAS) {
                continue;
            }
            ProductRequestContext context = new ProductRequestContext(platform, PLAYER_ID, GAME_TYPE, request);
            Currency currency = underTest.getPreferredCurrency(context);

            assertThat(currency, is(Currency.USD));
        }
    }

    @Test
    public void shouldDelegateToFacebookCurrencyServiceWhenPlatformIsFACEBOOK_CANVAS() {
        ProductRequestContext context = new ProductRequestContext(Platform.FACEBOOK_CANVAS, PLAYER_ID, GAME_TYPE, request);
        Mockito.when(facebookCurrencyService.getPreferredCurrencyFromFacebook(request, GAME_TYPE)).thenReturn(Currency.AFN);

        Currency actualCurrency = underTest.getPreferredCurrency(context);

        assertThat(actualCurrency, is(Currency.AFN));
    }
}
