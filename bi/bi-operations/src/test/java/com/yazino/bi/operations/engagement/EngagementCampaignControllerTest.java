package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EngagementCampaignControllerTest {

    private static final Integer ID1 = 1;
    private static final String MESSAGE1 = "Howdy!";

    @Mock
    private EngagementCampaignDao dao;

    @Mock
    private EngagementCampaignSender appRequestSender;

    private EngagementCampaignSenderController underTest;
    private Model model;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new EngagementCampaignSenderController(dao, appRequestSender);
        model = new ExtendedModelMap();
    }

    @Test
    public void testSendPushNotificationForIdOfTypeIos() {
        EngagementCampaign push1 = new EngagementCampaignBuilder().withId(ID1).withMessage(MESSAGE1).withChannelType(ChannelType.IOS).build();
        when(dao.findById(ID1)).thenReturn(push1);

        underTest.sendAppRequestForId(model, ID1);
        verify(appRequestSender).sendAppRequest(push1);
    }


    @Test
    public void testSendPushNotificationForIdOfTypeFacbook() {
        EngagementCampaign request = new EngagementCampaignBuilder().withId(ID1).withMessage(MESSAGE1).withChannelType(
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST).build();
        when(dao.findById(ID1)).thenReturn(request);

        underTest.sendAppRequestForId(model, ID1);
        verify(appRequestSender).sendAppRequest(request);
    }
}
