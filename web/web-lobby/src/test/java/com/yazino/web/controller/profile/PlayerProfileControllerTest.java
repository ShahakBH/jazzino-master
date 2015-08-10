package com.yazino.web.controller.profile;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.player.*;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.spring.mvc.DateTimeEditor;
import com.yazino.util.DateUtils;
import com.yazino.web.data.CountryRepository;
import com.yazino.web.data.GenderRepository;
import com.yazino.web.domain.AvatarRepository;
import com.yazino.web.domain.DisplayName;
import com.yazino.web.domain.EmailAddress;
import com.yazino.web.domain.PlayerProfileSummaryBuilder;
import com.yazino.web.form.PasswordChangeFormTestBuilder;
import com.yazino.web.form.validation.PasswordChangeFormValidator;
import com.yazino.web.service.PlayerProfileCacheRemovalListener;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.LobbyExceptionHandler;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PlayerProfileControllerTest {

    private final String countryCode = "BOB";
    private final String expectedCountry = "Bob The Builders Wonder Land";

    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private PlayerService playerService;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private AvatarRepository avatarRepository;
    @Mock
    private LobbyExceptionHandler lobbyExceptionHandler;
    @Mock
    private FacebookConfiguration facebookConfiguration;
    @Mock
    private PasswordChangeFormValidator passwordChangeFormValidator;
    @Mock
    private PlayerProfileCacheRemovalListener playerProfileCacheRemovalListener;
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private GenderRepository genderRepository;
    @Mock
    private CountryRepository countryRepository;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private PlayerProfileController underTest;
    private Map<String, String> expectedCountries = Collections.singletonMap(countryCode, expectedCountry);
    private ModelMap modelMap;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(yazinoConfiguration.getBoolean(anyString())).thenReturn(true);

        when(countryRepository.getCountries()).thenReturn(expectedCountries);
        modelMap = new ModelMap();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        underTest = new PlayerProfileController(lobbySessionCache, playerProfileService,
                avatarRepository, lobbyExceptionHandler, passwordChangeFormValidator,
                playerProfileCacheRemovalListener, yazinoConfiguration, genderRepository, countryRepository, playerService);
    }

    @Test
    public void shouldReturnCorrectViewForMainProfilePage() {
        setUpPlayerProfile();
        assertMainProfilePageViewName(underTest.getMainProfilePage(modelMap, request, response, false));
    }

    @Test
    public void shouldReturnCorrectViewForMainProfilePageAsPartial() {
        setUpPlayerProfile();
        assertMainProfilePagePartialViewName(underTest.getMainProfilePage(modelMap, request, response, true));
    }

    @Test
    public void shouldReturnUserProfileForMainProfilePage() {
        PlayerProfile expectedUserProfile = setUpPlayerProfile();
        underTest.getMainProfilePage(modelMap, request, response, true);
        assertModelMapContainsPlayerObject(expectedUserProfile);
    }

    @Test
    public void shouldContainAvailableTabs() {
        setUpPlayerProfile();
        ProfileTestHelper.assertFullListOfTabsInModel(underTest.getMainProfilePage(modelMap, request, response, true));
    }

    @Test
    public void souldReturnCorrectPartialValueInModel() {
        setUpPlayerProfile();
        boolean expectedPartialValue = true;
        final ModelAndView mainProfilePage = underTest.getMainProfilePage(modelMap, request, response, expectedPartialValue);
        assertModelMapContainsPartialValue(expectedPartialValue, mainProfilePage.getModelMap());
    }

    private void assertModelMapContainsPartialValue(final boolean expectedPartialValue, final ModelMap modelMap) {
        assertEquals(expectedPartialValue, modelMap.get(AbstractProfileController.PARTIAL_URI_FLAG));
    }

    @Test
    public void shouldReturnUserProfileInfoForMainProfilePage() {
        PlayerProfile userProfile = setUpPlayerProfile();
        underTest.getMainProfilePage(modelMap, request, response, true);
        assertModelMapContainsPlayerInfoObject(userProfile);
    }

    @Test
    public void shouldReturnEmailAddressObjectForMainProfilePage() {
        PlayerProfile userProfile = setUpPlayerProfile();
        underTest.getMainProfilePage(modelMap, request, response, true);
        assertModelMapContainsEmailAddressObject(userProfile);
    }

    @Test
    public void shouldReturnDisplayNameObjectForMainProfilePage() {
        PlayerProfile userProfile = setUpPlayerProfile();
        underTest.getMainProfilePage(modelMap, request, response, true);
        assertModelMapContainsDisplayNameObject(userProfile);
    }

    @Test
    public void shouldReturnPasswordChangeObjectForMainProfilePage() {
        PlayerProfile userProfile = setUpPlayerProfile();
        underTest.getMainProfilePage(modelMap, request, response, true);
        assertModelMapContainsPasswordChangeFormObject(userProfile);
    }

    @Test
    public void shouldContainListOfCountriesFromResourceData() {
        final Map<String, String> actualCountries = underTest.populateCountries();
        assertEquals(expectedCountries, actualCountries);
    }

    @Test
    public void shouldGetCorrectDateYearList() {
        Map<String, String> map = underTest.populateMonthsYears();
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        assertEquals(101, map.size());
        assertEquals(Integer.toString(year), map.values().toArray()[0]);
        assertEquals(Integer.toString(year - 100), map.values().toArray()[100]);
    }

    @Test
    public void shouldGetCorrectListOfMonths() {
        DateUtils dateUtils = new DateUtils();
        final String[] months = dateUtils.getShortFormMonthsOfYear();
        final String[] keys = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        Map<String, String> actualMonths = underTest.populateMonths();
        assertEquals("expect size to be 12", 12, actualMonths.size());
        assertArrayEquals(months, actualMonths.values().toArray());
        assertArrayEquals(keys, actualMonths.keySet().toArray());
    }

    @Test
    public void shouldReturnCorrectGenders() {
        Map<String, String> actualGenders = underTest.populateGenders();
        assertThat(genderRepository.getGenders(), is(equalTo(actualGenders)));
    }

    @Test
    public void shouldUpdateDisplayNameForUser() {
        DisplayName expectedDisplayName = new DisplayName("Super Cool New Name");
        PlayerProfile userProfile = setupUpdateDisplayName(expectedDisplayName.getDisplayName());

        underTest.updateDisplayName(request, response, false, expectedDisplayName, bindingResult, modelMap);

        verify(playerProfileService).updateDisplayName(userProfile.getPlayerId(), expectedDisplayName.getDisplayName());
    }

    @Test
    public void shouldClearPlayerProfileCacheOnUpdateOfDisplayNameForUser() {
        final DisplayName expectedDisplayName = new DisplayName("Super Cool New Name");
        final PlayerProfile userProfile = setupUpdateDisplayName(expectedDisplayName.getDisplayName());

        underTest.updateDisplayName(request, response, false, expectedDisplayName, bindingResult, modelMap);

        verify(playerProfileCacheRemovalListener).playerUpdated(userProfile.getPlayerId());
    }

    @Test
    public void shouldUpdateDisplayNameForUserAndRedirectToOriginalForm() {
        DisplayName expectedDisplayName = new DisplayName("Super Cool New Name");
        setupUpdateDisplayName(expectedDisplayName.getDisplayName());

        assertRedirectToMainProfilePage(underTest.updateDisplayName(request, response, false, expectedDisplayName, bindingResult, modelMap));
        assertModelMapIsEmptyToAvoidQueryStringOnRedirect(modelMap);
    }

    @Test
    public void shouldUpdateEmailAddressForUser() {
        String expectedEmailAddress = "if.you.read.this.then@least.give.me.cake";

        PlayerProfile expectedUserProfile = setupUpdateEmailAddress(expectedEmailAddress);
        underTest.updateCommunicationEmailAddress(request, response, false, new EmailAddress(expectedUserProfile), bindingResult, modelMap);

        verify(playerProfileService).updateEmailAddress(expectedUserProfile.getPlayerId(), expectedEmailAddress);
    }

    @Test
    public void shouldClearPlayerProfileCacheOnUpdateEmailAddressForUser() {
        String expectedEmailAddress = "if.you.read.this.then@least.give.me.cake";

        PlayerProfile expectedUserProfile = setupUpdateEmailAddress(expectedEmailAddress);
        underTest.updateCommunicationEmailAddress(request, response, false,
                new EmailAddress(expectedUserProfile), bindingResult, modelMap);

        verify(playerProfileCacheRemovalListener).playerUpdated(expectedUserProfile.getPlayerId());
    }

    @Test
    public void shouldUpdateEmailAddressForUserAndReturnCorrectView() {
        String expectedEmailAddress = "if.you.read.this.then@least.give.me.cake";
        setupUpdateEmailAddress(expectedEmailAddress);

        assertRedirectToMainProfilePage(underTest.updateCommunicationEmailAddress(request, response, false, new EmailAddress(expectedEmailAddress), bindingResult, modelMap));
        assertModelMapIsEmptyToAvoidQueryStringOnRedirect(modelMap);
    }

    @Test
    public void shouldUpdateUserProfileInfoForUser() {
        String expectedAvatarUrl = "http://lol/avatar";
        String expectedGender = "M";
        String expectedCountry = "Brerea";
        DateTime expectedDateOfBirth = new DateTime(System.currentTimeMillis());

        PlayerProfile expectedUserProfile = setUpUserProfileInfo(expectedAvatarUrl, expectedGender, expectedCountry, expectedDateOfBirth);
        PlayerProfileSummary expectedUserProfileInfo = new PlayerProfileSummaryBuilder(expectedUserProfile).build();
        underTest.updateUserProfileInfo(request, response, false, expectedUserProfileInfo, bindingResult, modelMap);

        verify(playerProfileService).updatePlayerInfo(expectedUserProfile.getPlayerId(), expectedUserProfileInfo);
    }

    @Test
    public void shouldClearPlayerProfileCacheOnUpdateUserProfileInfoForUser() {
        String expectedAvatarUrl = "http://lol/avatar";
        String expectedGender = "M";
        String expectedCountry = "Brerea";
        DateTime expectedDateOfBirth = new DateTime(System.currentTimeMillis());

        PlayerProfile expectedUserProfile = setUpUserProfileInfo(
                expectedAvatarUrl, expectedGender, expectedCountry, expectedDateOfBirth);
        PlayerProfileSummary expectedUserProfileInfo = new PlayerProfileSummaryBuilder(expectedUserProfile).build();
        underTest.updateUserProfileInfo(request, response, false, expectedUserProfileInfo, bindingResult, modelMap);

        verify(playerProfileCacheRemovalListener).playerUpdated(expectedUserProfile.getPlayerId());
    }

    @Test
    public void shouldUpdateUserProfileInfoForUserAndReturnCorrectView() {
        String expectedAvatarUrl = "http://lol/avatar";
        String expectedGender = "M";
        String expectedCountry = "Brerea";
        DateTime expectedDateOfBirth = new DateTime(System.currentTimeMillis());

        PlayerProfile expectedUserProfile = setUpUserProfileInfo(expectedAvatarUrl, expectedGender, expectedCountry, expectedDateOfBirth);
        PlayerProfileSummary expectedUserProfileInfo = new PlayerProfileSummaryBuilder(expectedUserProfile).build();
        when(bindingResult.hasErrors()).thenReturn(false);

        assertRedirectToMainProfilePage(underTest.updateUserProfileInfo(request, response, false, expectedUserProfileInfo, bindingResult, modelMap));
        assertModelMapIsEmptyToAvoidQueryStringOnRedirect(modelMap);
    }

    @Test
    public void shouldRegisterCorrectDateTimeEditor() {
        WebDataBinder binder = spy(new WebDataBinder(null));
        underTest.initBinder(binder);
        verify(binder).registerCustomEditor(eq(DateTime.class), any(DateTimeEditor.class));
    }

    @Test
    public void shouldOnErrorsForDisplayNameReturnMainFormAgain() {
        DisplayName expectedDisplayName = new DisplayName("Super Cool New Name");
        setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(true);
        assertMainProfilePageViewName(underTest.updateDisplayName(request, response, false, expectedDisplayName, bindingResult, modelMap));
    }

    @Test
    public void shouldOnUpdateDisplayNameNotUpdateAnythingIfErrors() {
        DisplayName expectedDisplayName = new DisplayName("Super Cool New Name");
        setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(true);
        underTest.updateDisplayName(request, response, false, expectedDisplayName, bindingResult, modelMap);
        verifyNoUpdatesDone();
    }

    @Test
    public void shouldOnUpdateDisplayNameReturnCorrectPartialValue() {
        DisplayName expectedDisplayName = new DisplayName("Super Cool New Name");
        setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(false);
        ModelAndView modelAndView = underTest.updateDisplayName(request, response, true, expectedDisplayName, bindingResult, modelMap);
        assertTrue(((RedirectView) modelAndView.getView()).getUrl().endsWith("?partial=true"));
    }

    @Test
    public void shouldOnUpdateCommunicationEmailReturnCorrectPartialValue() {
        EmailAddress expectedEmailAddress = new EmailAddress("dont@@hit.me");
        setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(false);
        ModelAndView modelAndView = underTest.updateCommunicationEmailAddress(request, response, true, expectedEmailAddress, bindingResult, modelMap);
        assertTrue(((RedirectView) modelAndView.getView()).getUrl().endsWith("?partial=true"));
    }

    @Test
    public void shouldOnUpdateUserProfileReturnCorrectPartialValue() {
        PlayerProfile expectedUserProfile = setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(false);
        ModelAndView modelAndView = underTest.updateUserProfileInfo(request, response, true, new PlayerProfileSummaryBuilder(expectedUserProfile).build(), bindingResult, modelMap);
        assertTrue(((RedirectView) modelAndView.getView()).getUrl().endsWith("?partial=true"));
    }

    @Test
    public void shouldOnUpdateAvatarReturnCorrectPartialValue() throws Exception {
        setUpPlayerProfile();

        MultipartFile multipartFile = mock(MultipartFile.class);
        String expectedOriginalFilename = "WORD";
        byte[] expectedBytes = expectedOriginalFilename.getBytes();

        when(multipartFile.getBytes()).thenReturn(expectedBytes);
        when(multipartFile.getOriginalFilename()).thenReturn(expectedOriginalFilename);

        ModelAndView modelAndView = underTest.updateUserAvatar(request, response, true, multipartFile, modelMap);
        assertTrue(((RedirectView) modelAndView.getView()).getUrl().endsWith("?partial=true"));
    }

    @Test
    public void shouldClearPlayerProfileCacheOnAvatarUpdate() throws Exception {
        final PlayerProfile playerProfile = setUpPlayerProfile();

        final MultipartFile multipartFile = mock(MultipartFile.class);
        final String expectedOriginalFilename = "WORD";
        final byte[] expectedBytes = expectedOriginalFilename.getBytes();

        when(multipartFile.getBytes()).thenReturn(expectedBytes);
        when(multipartFile.getOriginalFilename()).thenReturn(expectedOriginalFilename);

        when(avatarRepository.storeAvatar(expectedOriginalFilename, expectedBytes))
                .thenReturn(new Avatar("one", "two"));

        underTest.updateUserAvatar(request, response, true, multipartFile, modelMap);

        verify(playerProfileCacheRemovalListener).playerUpdated(playerProfile.getPlayerId());
    }

    @Test
    public void shouldOnUpdatePasswordReturnCorrectPartialValue() {
        PlayerProfile userProfile = setUpPlayerProfile();
        PasswordChangeRequest passwordChangeForm = setUpPasswordUpdate();
        when(bindingResult.hasErrors()).thenReturn(false);
        ModelAndView modelAndView = underTest.updatedPassword(request, response, true, passwordChangeForm, bindingResult, modelMap);
        assertTrue(((RedirectView) modelAndView.getView()).getUrl().endsWith("?partial=true"));
    }

    private void verifyNoUpdatesDone() {
        verify(bindingResult, times(1)).hasErrors();
        verify(bindingResult, atLeastOnce()).rejectValue(anyString(), anyString(), anyString());
        verify(playerProfileService, never()).updatePlayerInfo(any(BigDecimal.class), any(PlayerProfileSummary.class));
    }

    @Test
    public void shouldOnErrorsForDisplayNameReturnPlayerObjectAgain() {
        DisplayName expectedDisplayName = new DisplayName("Super Cool New Name");
        PlayerProfile expectedUserProfile = setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(true);
        underTest.updateDisplayName(request, response, false, expectedDisplayName, bindingResult, modelMap);
        assertModelMapContainsPlayerObject(expectedUserProfile);
    }

    @Test
    public void shouldOnUpdateUserProfileInfoNotUpdateAnythingIfErrors() {
        PlayerProfile expectedUserProfile = setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(true);
        underTest.updateUserProfileInfo(request, response, false, new PlayerProfileSummaryBuilder(expectedUserProfile).build(), bindingResult, modelMap);
        verifyNoUpdatesDone();
    }

    @Test
    public void shouldOnErrorsForPlayerInfoReturnMainFormAgain() {
        PlayerProfile expectedUserProfile = setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(true);
        assertMainProfilePageViewName(underTest.updateUserProfileInfo(request, response, false, new PlayerProfileSummaryBuilder(expectedUserProfile).build(), bindingResult, modelMap));
    }

    @Test
    public void shouldOnErrorsForPlayerInfoReturnPlayerObjectAgain() {
        PlayerProfile expectedUserProfile = setUpPlayerProfile();
        when(bindingResult.hasErrors()).thenReturn(true);
        underTest.updateUserProfileInfo(request, response, false, new PlayerProfileSummaryBuilder(expectedUserProfile).build(), bindingResult, modelMap);
        assertModelMapContainsPlayerObject(expectedUserProfile);
    }

    @Test
    public void shouldOnErrorsForUpdateEmailAddressReturnPlayerObjectAgain() {
        EmailAddress expectedEmailAddress = new EmailAddress("dont@@hit.me");
        PlayerProfile expectedUserProfile = setupUpdateEmailAddress(expectedEmailAddress.getEmailAddress());
        when(bindingResult.hasErrors()).thenReturn(true);
        underTest.updateCommunicationEmailAddress(request, response, false, expectedEmailAddress, bindingResult, modelMap);
        assertModelMapContainsPlayerObject(expectedUserProfile);
    }

    @Test
    public void shouldOnEmailAddressNotUpdateAnythingIfErrors() {
        EmailAddress expectedEmailAddress = new EmailAddress("invalid");
        setupUpdateEmailAddress(expectedEmailAddress.getEmailAddress());
        when(bindingResult.hasErrors()).thenReturn(true);
        underTest.updateCommunicationEmailAddress(request, response, false, expectedEmailAddress, bindingResult, modelMap);
        verifyNoUpdatesDone();
    }

    @Test
    public void shouldOnErrorsForUpdateEmailAddressReturnMainFormAgain() {
        EmailAddress expectedEmailAddress = new EmailAddress("invalid");
        setupUpdateEmailAddress(expectedEmailAddress.getEmailAddress());
        when(bindingResult.hasErrors()).thenReturn(true);
        assertMainProfilePageViewName(underTest.updateCommunicationEmailAddress(request, response, false, expectedEmailAddress, bindingResult, modelMap));
    }

    @Test
    public void shouldNotAddPlayerInfoIfInMap() {
        String expectedAttribute = "My Object";
        modelMap.addAttribute(PlayerProfileController.PLAYER_INFO_OBJECT_KEY, expectedAttribute);
        setUpPlayerProfile();
        underTest.getMainProfilePage(modelMap, request, response, true);
        assertEquals(expectedAttribute, modelMap.get(PlayerProfileController.PLAYER_INFO_OBJECT_KEY));
    }

    @Test
    public void shouldNotAddDisplayNameIfInMap() {
        String expectedAttribute = "My Object";
        modelMap.addAttribute(PlayerProfileController.DISPLAY_NAME_OBJECT_KEY, expectedAttribute);
        setUpPlayerProfile();
        underTest.getMainProfilePage(modelMap, request, response, true);
        assertEquals(expectedAttribute, modelMap.get(PlayerProfileController.DISPLAY_NAME_OBJECT_KEY));
    }

    @Test
    public void shouldNotAddEmailAddressIfInMap() {
        String expectedAttribute = "My Object";
        modelMap.addAttribute(PlayerProfileController.EMAIL_ADDRESS_OBJECT_KEY, expectedAttribute);
        setUpPlayerProfile();
        underTest.getMainProfilePage(modelMap, request, response, true);
        assertEquals(expectedAttribute, modelMap.get(PlayerProfileController.EMAIL_ADDRESS_OBJECT_KEY));
    }

    @Test
    public void shouldStoreAvatarFile() throws Exception {
        setUpPlayerProfile();

        MultipartFile multipartFile = mock(MultipartFile.class);
        String expectedOriginalFilename = "WORD";
        byte[] expectedBytes = expectedOriginalFilename.getBytes();

        when(multipartFile.getBytes()).thenReturn(expectedBytes);
        when(multipartFile.getOriginalFilename()).thenReturn(expectedOriginalFilename);

        underTest.updateUserAvatar(request, response, false, multipartFile, modelMap);

        verify(avatarRepository, times(1)).storeAvatar(expectedOriginalFilename, expectedBytes);
    }

    @Test
    public void shouldUpdateAvatarUrl() throws Exception {
        PlayerProfile userProfile = setUpPlayerProfile();

        MultipartFile multipartFile = mock(MultipartFile.class);
        String expectedOriginalFilename = "WORD";
        byte[] expectedBytes = expectedOriginalFilename.getBytes();

        when(multipartFile.getBytes()).thenReturn(expectedBytes);
        when(multipartFile.getOriginalFilename()).thenReturn(expectedOriginalFilename);

        Avatar expectedAvatar = new Avatar("one", "two");
        when(avatarRepository.storeAvatar(expectedOriginalFilename, expectedBytes)).thenReturn(expectedAvatar);

        underTest.updateUserAvatar(request, response, false, multipartFile, modelMap);

        verify(playerProfileService).updateAvatar(userProfile.getPlayerId(), expectedAvatar);
    }

    @Test
    public void shouldRedirectOnSuccessfulUpdateOfAvatarUrl() throws Exception {
        setUpPlayerProfile();
        MultipartFile multipartFile = mock(MultipartFile.class);
        String expectedOriginalFilename = "WORD";
        byte[] expectedBytes = expectedOriginalFilename.getBytes();

        when(multipartFile.getBytes()).thenReturn(expectedBytes);
        when(multipartFile.getOriginalFilename()).thenReturn(expectedOriginalFilename);

        assertRedirectToMainProfilePage(underTest.updateUserAvatar(request, response, false, multipartFile, modelMap));
        assertModelMapIsEmptyToAvoidQueryStringOnRedirect(modelMap);
    }

    @Test
    public void shouldReturnDefaultFormOnErrorWhenUpdatingAvatarUrl() throws Exception {
        setUpPlayerProfile();
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getBytes()).thenThrow(new IOException());

        assertMainProfilePageViewName(underTest.updateUserAvatar(request, response, false, multipartFile, modelMap));
    }

    @Test
    public void shouldReturnDefaultFormOnErrorWhenAvatarHasAnInvalidExtension() throws Exception {
        setUpPlayerProfile();
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getBytes()).thenReturn("file".getBytes());
        when(multipartFile.getOriginalFilename()).thenReturn("aFile.pdf");
        when(avatarRepository.storeAvatar("aFile.pdf", "file".getBytes())).thenThrow(
                new IllegalArgumentException("Invalid extension"));

        assertMainProfilePageViewName(underTest.updateUserAvatar(request, response, false, multipartFile, modelMap));
        assertThat((String) modelMap.get("avatarHasErrors"), is(equalTo("true")));
    }

    @Test
    public void shouldReturnDefaultViewOnErrorWhenMaxUploadSizeExceeded() {
        setUpPlayerProfile();
        long maxUploadSize = 1000;

        DisplayName expectedDisplayName = new DisplayName("displayName");
        PlayerProfile expectedUserProfile = getUserProfile();
        ModelAndView modelAndView = underTest.resolveException(request, response, null, new MaxUploadSizeExceededException(maxUploadSize));
        verify(lobbyExceptionHandler).setCommonPropertiesInModelAndView(request, response, modelAndView);

        final ModelMap modelMap = modelAndView.getModelMap();
        assertEquals(expectedDisplayName, modelMap.get(PlayerProfileController.DISPLAY_NAME_OBJECT_KEY));
        assertEquals(expectedUserProfile, modelMap.get(PlayerProfileController.PLAYER_OBJECT_KEY));
        assertMainProfilePageViewName(modelAndView);
    }

    @Test
    public void resolveExceptionShouldReturnNullForExceptionsExceptMaxUploadSizeExceeded() {
        assertNull(underTest.resolveException(request, response, null, new Exception()));
    }

    @Test
    public void resolveExceptionShouldSetPartial() {
        long maxUploadSize = 1000;
        setUpPlayerProfile();

        Boolean expectedPartialValue = Boolean.TRUE;
        request.setParameter(AbstractProfileController.PARTIAL_URI_FLAG, "true");
        ModelAndView modelAndViewUnderTest = underTest.resolveException(request, response, null, new MaxUploadSizeExceededException(maxUploadSize));

        assertEquals(expectedPartialValue, modelAndViewUnderTest.getModel().get(AbstractProfileController.PARTIAL_URI_FLAG));

        request.setParameter(AbstractProfileController.PARTIAL_URI_FLAG, "false");
        modelAndViewUnderTest = underTest.resolveException(request, response, null, new MaxUploadSizeExceededException(maxUploadSize));
        ProfileTestHelper.assertPartialFlagSetCorrectly(modelAndViewUnderTest, false);
    }

    @Test
    public void resolveExceptionShouldSetGender() {
        long maxUploadSize = 1000;
        setUpPlayerProfile();

        final ModelAndView modelAndViewUnderTest = underTest.resolveException(request, response, null, new MaxUploadSizeExceededException(maxUploadSize));
        assertThat(genderRepository.getGenders(), is(equalTo(modelAndViewUnderTest.getModelMap().get("genders"))));
    }

    @Test
    public void resolveExceptionShouldSetCountries() {
        long maxUploadSize = 1000;
        setUpPlayerProfile();

        final ModelAndView modelAndViewUnderTest = underTest.resolveException(request, response, null, new MaxUploadSizeExceededException(maxUploadSize));
        assertSame(countryRepository.getCountries(), modelAndViewUnderTest.getModelMap().get("countries"));
    }


    @Test
    public void shouldOnPasswordUpdateReplacePlayerId() {
        PlayerProfile userProfile = setUpPlayerProfile();
        PasswordChangeRequest passwordChangeForm = setUpPasswordUpdate();
        when(bindingResult.hasErrors()).thenReturn(false);
        underTest.updatedPassword(request, response, false, passwordChangeForm, bindingResult, modelMap);
        PasswordChangeRequest expectedPasswordChangeForm = new PasswordChangeFormTestBuilder(passwordChangeForm).withPlayerId(userProfile.getPlayerId()).build();
        verify(playerProfileService).updatePassword(userProfile.getPlayerId(), expectedPasswordChangeForm);
    }

    @Test
    public void shouldRedirectOnSuccessfulUpdateOfPassword() {
        PlayerProfile userProfile = setUpPlayerProfile();
        PasswordChangeRequest passwordChangeForm = setUpPasswordUpdate();
        when(bindingResult.hasErrors()).thenReturn(false);
        assertRedirectToMainProfilePage(underTest.updatedPassword(request, response, false, passwordChangeForm, bindingResult, modelMap));
        assertModelMapIsEmptyToAvoidQueryStringOnRedirect(modelMap);
    }

    @Test
    public void shouldOnUpdatePasswordNotUpdateAnythingIfErrors() {
        PlayerProfile userProfile = setUpPlayerProfile();
        PasswordChangeRequest passwordChangeForm = setUpPasswordUpdate();
        passwordChangeForm = new PasswordChangeFormTestBuilder(passwordChangeForm).withConfirmNewPassword("bob").build();
        when(bindingResult.hasErrors()).thenReturn(true);
        underTest.updatedPassword(request, response, false, passwordChangeForm, bindingResult, modelMap);
        verify(passwordChangeFormValidator).validate(passwordChangeForm, bindingResult);
        verify(playerProfileService, never()).updatePlayerInfo(any(BigDecimal.class), any(PlayerProfileSummary.class));
    }

    @Test
    public void shouldOnUpdatePasswordIfErrorsReturnResetPasswordChangeForm() {
        PlayerProfile userProfile = setUpPlayerProfile();
        PasswordChangeRequest passwordChangeForm = setUpPasswordUpdate();
        passwordChangeForm = new PasswordChangeFormTestBuilder(passwordChangeForm).withConfirmNewPassword("bob").build();

        when(bindingResult.hasErrors()).thenReturn(true);
        underTest.updatedPassword(request, response, false, passwordChangeForm, bindingResult, modelMap);

        PasswordChangeRequest expectedPasswordChangeForm = new PasswordChangeFormTestBuilder(passwordChangeForm).build();
        expectedPasswordChangeForm.clear();

        assertEquals(expectedPasswordChangeForm, passwordChangeForm);
    }

    @Test
    public void shouldOnErrorsForUpdatePasswordReturnMainFormAgain() {
        PlayerProfile userProfile = setUpPlayerProfile();
        PasswordChangeRequest passwordChangeForm = setUpPasswordUpdate();
        when(bindingResult.hasErrors()).thenReturn(true);
        assertMainProfilePageViewName(underTest.updatedPassword(request, response, false, passwordChangeForm, bindingResult, modelMap));
    }

    @Test
    public void shouldOnUpdateFbSyncReturnCorrectPartial() {
        setUpPlayerProfile();
        ModelAndView modelAndView = underTest.updateFbSync(request, response, true, false, modelMap);
        assertTrue(((RedirectView) modelAndView.getView()).getUrl().endsWith("?partial=true"));
    }

    @Test
    public void shouldOnUpdateFbSyncUpdateUpdateFbSyncForPlayer() {
        PlayerProfile userProfile = setUpPlayerProfile();
        boolean expectedFbSync = true;
        underTest.updateFbSync(request, response, false, expectedFbSync, modelMap);
        verify(playerProfileService, times(1)).updateSyncFor(userProfile.getPlayerId(), expectedFbSync);
    }

    @Test
    public void shouldOnUpdateAvatarNotHavingFileNameReturnDefaultInfo() {
        setUpPlayerProfile();

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("");

        underTest.updateUserAvatar(request, response, true, multipartFile, modelMap);
        verify(playerProfileService, never()).updateAvatar(any(BigDecimal.class), any(Avatar.class));
    }
    private PasswordChangeRequest setUpPasswordUpdate() {
        PasswordChangeRequest passwordChangeForm = new PasswordChangeFormTestBuilder().withConfirmNewPassword(PasswordChangeFormTestBuilder.NEW_PASSWORD).withPlayerId(BigDecimal.ONE).build();
        when(bindingResult.hasErrors()).thenReturn(true);
        return passwordChangeForm;
    }

    private void assertMainProfilePageViewName(ModelAndView actualModelAndView) {
        ProfileTestHelper.assertNonPartialLayout(actualModelAndView);
        ProfileTestHelper.assertSelectTabIs(actualModelAndView, "player");
    }

    private void assertMainProfilePagePartialViewName(ModelAndView actualModelAndView) {
        ProfileTestHelper.assertPartialLayout(actualModelAndView);
        ProfileTestHelper.assertSelectTabIs(actualModelAndView, "player");
    }

    private void assertRedirectToMainProfilePage(ModelAndView modelAndView) {
        View expectedViewReturnValue = new RedirectView("/player/profile", false, false, false);
        View actualViewReturnValue = modelAndView.getView();
        assertEquals("Redirect should go to the expected URI", actualViewReturnValue.toString(), expectedViewReturnValue.toString());
    }

    private void assertModelMapIsEmptyToAvoidQueryStringOnRedirect(ModelMap modelMap) {
        assertTrue(modelMap.isEmpty());
    }

    private void assertModelMapContainsPlayerInfoObject(PlayerProfile expectedUserProfile) {
        PlayerProfileSummary expectedUserProfileInfo = new PlayerProfileSummaryBuilder(expectedUserProfile).build();
        assertEquals(expectedUserProfileInfo, modelMap.get(PlayerProfileController.PLAYER_INFO_OBJECT_KEY));
    }

    private void assertModelMapContainsEmailAddressObject(PlayerProfile userProfile) {
        EmailAddress expectedEmailAddress = new EmailAddress(userProfile);
        assertEquals(expectedEmailAddress, modelMap.get(PlayerProfileController.EMAIL_ADDRESS_OBJECT_KEY));
    }

    private void assertModelMapContainsDisplayNameObject(PlayerProfile userProfile) {
        DisplayName expectedDisplayName = new DisplayName(userProfile);
        assertEquals(expectedDisplayName, modelMap.get(PlayerProfileController.DISPLAY_NAME_OBJECT_KEY));
    }

    private void assertModelMapContainsPasswordChangeFormObject(PlayerProfile userProfile) {
        PasswordChangeRequest expectedPasswordChangeForm = new PasswordChangeRequest(userProfile);
        assertEquals(expectedPasswordChangeForm, modelMap.get(PlayerProfileController.PASSWORD_CHANGE_OBJECT_KEY));
    }

    private void assertModelMapContainsPlayerObject(PlayerProfile expectedUserProfile) {
        assertEquals(expectedUserProfile, modelMap.get(PlayerProfileController.PLAYER_OBJECT_KEY));
    }

    private PlayerProfile setUpPlayerProfile() {
        PlayerProfile userProfile = getUserProfile();
        when(lobbySessionCache.getActiveSession(request)).thenReturn(getLobbySession(userProfile));
        when(playerProfileService.findByPlayerId(userProfile.getPlayerId())).thenReturn(userProfile);
        return userProfile;
    }

    private PlayerProfile setupUpdateDisplayName(String displayName) {
        PlayerProfile userProfile = getUserProfile();
        when(lobbySessionCache.getActiveSession(request)).thenReturn(getLobbySession(userProfile));
        PlayerProfile finalUserProfile = getUserProfileWithDisplayName(displayName);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(getLobbySession(finalUserProfile));
        when(playerProfileService.findByPlayerId(userProfile.getPlayerId())).thenReturn(finalUserProfile);
        return finalUserProfile;
    }

    private PlayerProfile setupUpdateEmailAddress(String emailAddress) {
        PlayerProfile userProfile = getUserProfile();
        when(lobbySessionCache.getActiveSession(request)).thenReturn(getLobbySession(userProfile));
        PlayerProfile finalUserProfile = getUserProfileWithEmailAddress(emailAddress);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(getLobbySession(finalUserProfile));
        when(playerProfileService.findByPlayerId(userProfile.getPlayerId())).thenReturn(finalUserProfile);
        return finalUserProfile;
    }

    private PlayerProfile setUpUserProfileInfo(final String avatarUrl,
                                               final String gender,
                                               final String country,
                                               final DateTime dateOfBirth) {

        PlayerProfile userProfile = getUserProfile();
        when(lobbySessionCache.getActiveSession(request)).thenReturn(getLobbySession(userProfile));
        PlayerProfile finalUserProfile = PlayerProfile.copy(userProfile)
                .withGender(Gender.getById(gender))
                .withCountry(country)
                .withDateOfBirth(dateOfBirth)
                .asProfile();

        when(lobbySessionCache.getActiveSession(request)).thenReturn(getLobbySession(finalUserProfile));
        when(playerProfileService.findByPlayerId(userProfile.getPlayerId())).thenReturn(finalUserProfile);
        return finalUserProfile;
    }

    private LobbySession getLobbySession(PlayerProfile userProfile) {
        return new LobbySession(BigDecimal.valueOf(3141592), userProfile.getPlayerId(),
                userProfile.getDisplayName(),
                "session",
                Partner.YAZINO,
                "anAvatarUrl",
                userProfile.getEmailAddress(),
                null, false,
                WEB,
                AuthProvider.YAZINO);
    }

    private PlayerProfile getUserProfileWithDisplayName(String displayName) {
        return PlayerProfileTestBuilder.create().withDisplayName(displayName).asProfile();
    }

    private PlayerProfile getUserProfileWithEmailAddress(String emailAddress) {
        return PlayerProfileTestBuilder.create().withEmailAddress(emailAddress).asProfile();
    }

    private PlayerProfile getUserProfile() {
        return PlayerProfileTestBuilder.create().asProfile();
    }
}
