package com.yazino.bi.operations.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.bi.operations.util.SecurityInformationHelper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HomeControllerTest {
    @Mock
    private HttpServletResponse response;
    @Mock
    private SecurityInformationHelper securityInformationHelper;

    private final Set<String> roles = new HashSet<String>();

    private HomeController underTest;

    @Before
    public void setUp() {
        when(securityInformationHelper.getAccessRoles()).thenReturn(roles);

        underTest = new HomeController(securityInformationHelper);
    }

    @Test
    public void theUserShouldBeRedirectedToThePlayerSearchPageIfTheyHaveTheSupportRole()
            throws IOException {
        roles.addAll(asList("ROLE_SUPPORT", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ROLE_ROOT"));

        assertThat(underTest.home(response), is(equalTo("redirect:/playerSearch")));
    }

    @Test
    public void theUserShouldBeRedirectedToThePlayerSearchPageIfTheyHaveTheSupportManagerRole()
            throws IOException {
        roles.addAll(asList("ROLE_SUPPORT_MANAGER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ROLE_ROOT"));

        assertThat(underTest.home(response), is(equalTo("redirect:/playerSearch")));
    }

    @Test
    public void theUserShouldBeRedirectedToTheAdTrackingReportIfTheyHaveTheManagementRole()
            throws IOException {
        roles.addAll(asList("ROLE_MANAGEMENT", "ROLE_MARKETING", "ROLE_ROOT"));

        assertThat(underTest.home(response), is(equalTo("redirect:/adTrackingReportDefinition")));
    }

    @Test
    public void theUserShouldBeRedirectedToTheAdTrackingReportIfTheyHaveTheAdTrackingRole()
            throws IOException {
        roles.addAll(asList("ROLE_AD_TRACKING", "ROLE_MARKETING", "ROLE_ROOT"));

        assertThat(underTest.home(response), is(equalTo("redirect:/adTrackingReportDefinition")));
    }

    @Test
    public void theUserShouldBeRedirectedToThePromotionListIfTheyHaveTheMarketingRole()
            throws IOException {
        roles.addAll(asList("ROLE_MARKETING", "ROLE_ROOT"));

        assertThat(underTest.home(response), is(equalTo("redirect:/promotion/list")));
    }

    @Test
    public void theUserShouldBeRedirectedToTheUserAdminPageIfTheyHaveTheRootRole()
            throws IOException {
        roles.addAll(asList("ROLE_ROOT"));

        assertThat(underTest.home(response), is(equalTo("redirect:/userAdmin")));
    }

    @Test
    public void theUserShouldBeSentAForbiddenStatusIfTheyHaveNoValidRoles()
            throws IOException {
        roles.addAll(asList("ROLE_INVALID"));

        assertThat(underTest.home(response), is(nullValue()));
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    
    @Test
    public void theUserShouldBeSentAForbiddenStatusIfTheyHaveNoRoles()
            throws IOException {
        assertThat(underTest.home(response), is(nullValue()));
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

}
