package com.yazino.web.api.payments;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.payment.ProductService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class ProductControllerTest {
    public static final String GAME_TYPE = "SLOTS";
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(123456);
    public static final Long PROMO_ID = 987L;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private WebApiResponses jsonWriter;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private ProductService productService;
    @Mock
    private Map<Platform, ProductService> productServices;

    private Set<Platform> supportedPlatforms = newHashSet(Platform.AMAZON, Platform.FACEBOOK_CANVAS);

    private ProductController underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        productServices = newHashMap();
        productServices.put(Platform.FACEBOOK_CANVAS, productService);
        productServices.put(Platform.AMAZON, productService);

        underTest = new ProductController(lobbySessionCache, jsonWriter, cookieHelper);

        underTest.setProductServices(productServices);

        when(cookieHelper.isOnCanvas(request, response)).thenReturn(true);
    }

    @Test
    public void requestProductsShouldReturnBadRequestWhenNotOnCanvasAndPlatformIsFACEBOOK_CANVAS() throws IOException {
        when(cookieHelper.isOnCanvas(request, response)).thenReturn(false);

        underTest.products(request, response, Platform.FACEBOOK_CANVAS.name(), GAME_TYPE);

        verify(jsonWriter, times(1)).writeError(response, SC_FORBIDDEN, "facebook payment only available to canvas apps");
    }

    @Test
    public void requestProductsShouldReturnBadRequestIfGameTypeIsMissing() throws IOException {
        underTest.products(request, response, Platform.FACEBOOK_CANVAS.name(), null);

        verify(jsonWriter, times(1)).writeError(response, SC_BAD_REQUEST, "parameter 'gameType' is missing");
    }

    @Test
    public void requestProductsShouldReturnBadRequestIfGameTypeIsBlank() throws IOException {
        underTest.products(request, response, Platform.FACEBOOK_CANVAS.name(), "    ");

        verify(jsonWriter, times(1)).writeError(response, SC_BAD_REQUEST, "parameter 'gameType' is missing");
    }

    @Test
    public void requestProductsShouldReturnUnauthorizedWhenNoSession() throws IOException {
        underTest.products(request, response, Platform.FACEBOOK_CANVAS.name(), GAME_TYPE);

        verify(jsonWriter, times(1)).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - no session");
    }

    @Test
    public void requestProductsShouldDelegateToProductService() throws IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        when(productService.getAvailableProducts(request, PLAYER_ID, GAME_TYPE)).thenReturn(new ChipProducts());

        underTest.products(request, response, Platform.FACEBOOK_CANVAS.name(), GAME_TYPE);

        verify(productService).getAvailableProducts(request, PLAYER_ID, GAME_TYPE);
    }

    @Test
    public void shouldWrite403ToResponseForUnsupportedPlatforms() throws IOException {
        for (Platform value : Platform.values()) {
            underTest.products(request, response, value.name(), GAME_TYPE);
            if (!supportedPlatforms.contains(value)) {
                verify(jsonWriter).writeError(response, HttpServletResponse.SC_FORBIDDEN, String.format("Unsupported platform: '%s'", value.name()));
                verifyNoMoreInteractions(lobbySessionCache, productService);
            }
        }
    }

    @Test
    public void requestProductsShouldWriteProductToResponse() throws IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        ChipProducts chipProducts = new ChipProducts();
        when(productService.getAvailableProducts(request, PLAYER_ID, GAME_TYPE)).thenReturn(chipProducts);

        underTest.products(request, response, Platform.FACEBOOK_CANVAS.name(), GAME_TYPE);

        verify(jsonWriter).writeOk(response, chipProducts);
    }

    @Test
    public void requestProductsShouldOrderProductsFromHighestChipsToLowest() throws IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        ChipProducts chipProducts = new ChipProducts();
        chipProducts.addChipProduct(new ChipProduct.ProductBuilder().withChips(BigDecimal.valueOf(2000)).build());
        chipProducts.addChipProduct(new ChipProduct.ProductBuilder().withChips(BigDecimal.valueOf(1000)).build());
        chipProducts.addChipProduct(new ChipProduct.ProductBuilder().withChips(BigDecimal.valueOf(3000)).build());
        when(productService.getAvailableProducts(request, PLAYER_ID, GAME_TYPE)).thenReturn(chipProducts);

        underTest.products(request, response, Platform.FACEBOOK_CANVAS.name(), GAME_TYPE);

        List<ChipProduct> actualChipProducts = chipProducts.getChipProducts();
        assertThat(actualChipProducts.get(0).getChips(), is(BigDecimal.valueOf(3000)));
        assertThat(actualChipProducts.get(1).getChips(), is(BigDecimal.valueOf(2000)));
        assertThat(actualChipProducts.get(2).getChips(), is(BigDecimal.valueOf(1000)));
    }

    private void setUpActiveSessionForPlayer(BigDecimal playerId) {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(
                new LobbySession(BigDecimal.valueOf(3141592), playerId, "", "", Partner.YAZINO, "", "", null, false, Platform.ANDROID, AuthProvider.YAZINO));
    }
}