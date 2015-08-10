package com.yazino.web.api;

import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.util.SpringErrorResponseFormatter;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayersResourceControllerTest {
    private static final Map<String, Object> ERROR_JSON = new HashMap<>();

    @Mock
    private PlayerProfileService profileService;
    @Mock
    private HttpServletResponse response;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private SpringErrorResponseFormatter springErrorResponseFormatter;

    private final BindingResult bindingResult = new MapBindingResult(new HashMap<>(), "players");

    private PlayersResourceController underTest;

    @Before
    public void setUp() {
        when(springErrorResponseFormatter.toJson(bindingResult)).thenReturn(ERROR_JSON);

        underTest = new PlayersResourceController(profileService, webApiResponses, springErrorResponseFormatter);
    }

    @Test
    public void shouldReturnFailureViewAndCode400IfProviderIsUnsupported() throws Exception {
        underTest.findPlayers(response, toForm("unknown", "1", "2", "3"), bindingResult);

        assertTrue(bindingResult.hasFieldErrors("provider"));
        assertEquals(bindingResult.getFieldError("provider").getCode(), "unsupported");
        assertEquals(bindingResult.getFieldError("provider").getDefaultMessage(), "provider is not supported");
        verify(webApiResponses).write(eq(response), eq(HttpServletResponse.SC_BAD_REQUEST), same(ERROR_JSON));
    }

    @Test
    public void shouldReturnSuccessViewWithMatchesWhenProviderIsSupported_email() throws Exception {
        Map<String, BigDecimal> matches = toMap("a@b.com", 1, "c@d.com", 2, "e@f.com", 3);
        when(profileService.findByEmailAddresses(any(String[].class))).thenReturn(matches);

        underTest.findPlayers(response, toForm("yazino", "1", "2", "3"), bindingResult);

        final Map<String, Object> responseJson = successJson();
        assertThat(playersFrom(responseJson).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnModelWithEmptyResultsWhenNoEmailsMatched() throws Exception {
        when(profileService.findByEmailAddresses(any(String[].class))).thenReturn(Collections.<String, BigDecimal>emptyMap());

        underTest.findPlayers(response, toForm("yazino", "a@b.com", "c@d.com", "e@f.com"), bindingResult);

        final Map<String, Object> responseJson = successJson();
        assertThat(playersFrom(responseJson).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAllPlayersWhenAllEmailsMatched() throws Exception {
        Map<String, BigDecimal> matches = toMap("a@b.com", 1, "c@d.com", 2, "e@f.com", 3);
        when(profileService.findByEmailAddresses(Matchers.<String>anyVararg())).thenReturn(matches);

        underTest.findPlayers(response, toForm("yazino", "a@b.com", "c@d.com", "e@f.com"), bindingResult);

        final Map<String, Object> responseJson = successJson();
        assertThat(playersFrom(responseJson), is(equalTo(matches)));
    }

    @Test
    public void shouldReturnOnlyPlayersThatEmailsMatched() throws Exception {
        Map<String, BigDecimal> matches = toMap("a@b.com", 1);
        when(profileService.findByEmailAddresses(Matchers.<String>anyVararg())).thenReturn(matches);

        underTest.findPlayers(response, toForm("yazino", "a@b.com", "c@d.com", "e@f.com"), bindingResult);

        final Map<String, Object> responseJson = successJson();
        assertThat(playersFrom(responseJson), is(equalTo(matches)));
    }

    @Test
    public void shouldReturnSuccessViewWithMatchesWhenProviderIsSupported_facebook() throws Exception {
        Map<String, BigDecimal> matches = toMap("1234", 1, "5678", 2, "9012", 3);
        when(profileService.findByProviderNameAndExternalIds(eq("facebook"), Matchers.<String>anyVararg())).thenReturn(matches);

        underTest.findPlayers(response, toForm("facebook", "1", "2", "3"), bindingResult);

        final Map<String, Object> responseJson = successJson();
        assertThat(playersFrom(responseJson), is(equalTo(matches)));
    }

    @Test
    public void shouldReturnModelWithEmptyResultsWhenNoExternalIdsMatched() throws Exception {
        when(profileService.findByProviderNameAndExternalIds(eq("facebook"), Matchers.<String>anyVararg())).thenReturn(Collections.<String, BigDecimal>emptyMap());

        underTest.findPlayers(response, toForm("facebook", "1", "2", "3"), bindingResult);

        final Map<String, Object> responseJson = successJson();
        assertThat(playersFrom(responseJson).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAllPlayersWhenAllExternalIdsMatched() throws Exception {
        Map<String, BigDecimal> matches = toMap("1234", 1, "5678", 2, "9012", 3);
        when(profileService.findByProviderNameAndExternalIds(eq("facebook"), Matchers.<String>anyVararg())).thenReturn(matches);

        underTest.findPlayers(response, toForm("facebook", "1234", "5678", "9012"), bindingResult);

        final Map<String, Object> responseJson = successJson();
        assertThat(playersFrom(responseJson), is(equalTo(matches)));
    }

    @Test
    public void shouldReturnOnlyPlayersThatExternalIdsMatched() throws Exception {
        Map<String, BigDecimal> matches = toMap("1234", 1);
        when(profileService.findByProviderNameAndExternalIds(eq("facebook"), Matchers.<String>anyVararg())).thenReturn(matches);

        underTest.findPlayers(response, toForm("facebook", "1234", "5678", "9012"), bindingResult);

        final Map<String, Object> responseJson = successJson();
        assertThat(playersFrom(responseJson), is(equalTo(matches)));
    }

    private static FindPlayersForm toForm(String provider, String... ids) {
        FindPlayersForm form = new FindPlayersForm();
        form.setProvider(provider);
        form.setProviderIds(StringUtils.join(ids, ","));
        return form;
    }

    private static Map<String, BigDecimal> toMap(Object... stringAndIds) {
        if (stringAndIds.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of entries");
        }
        Map<String, BigDecimal> entries = new HashMap<>();
        for (int i = 0; i < stringAndIds.length; i += 2) {
            String value = (String) stringAndIds[i];
            Integer playerId = (Integer) stringAndIds[i + 1];
            entries.put(value, new BigDecimal(playerId));
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> successJson() throws IOException {
        final ArgumentCaptor<Map> jsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), jsonCaptor.capture());
        return (Map<String, Object>) jsonCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> playersFrom(final Map<String, Object> responseJson) {
        return ((Map<String, BigDecimal>) responseJson.get("players"));
    }

}
