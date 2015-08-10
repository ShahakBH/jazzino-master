package com.yazino.web.interceptor;

import com.yazino.web.service.SystemMessageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: adjanogly
 * Date: Nov 23, 2010
 * Time: 11:49:20 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommonModelAttributesInterceptorTest {

    CommonModelAttributeInterceptor commonModelAttributeInterceptor;

    @Mock
	SystemMessageService systemMessageService;

    private ModelAndView modelAndView;
    private static final String EXPECTED_SYSTEM_MESSAGE = "expected System Message";

    @Before
    public void setup() {
        commonModelAttributeInterceptor  = new CommonModelAttributeInterceptor(systemMessageService);

    }

    @Test
    public void populateModelMapWhenSystemMessageFound() throws Exception {
       modelAndView = new ModelAndView();
       when(systemMessageService.getLatestSystemMessage()).thenReturn(EXPECTED_SYSTEM_MESSAGE);

       commonModelAttributeInterceptor.postHandle(null, null, null, modelAndView);

       verify(systemMessageService).getLatestSystemMessage();
       assertThat((String)modelAndView.getModelMap().get("systemMessage"), equalTo(EXPECTED_SYSTEM_MESSAGE));
    }

    @Test
    public void whenNoModelAndViewIsNullDontAddSystemMessage() throws Exception {
        modelAndView = null;
        commonModelAttributeInterceptor.postHandle(null, null, null, modelAndView);
        verify(systemMessageService, never()).getLatestSystemMessage();

    }
}
