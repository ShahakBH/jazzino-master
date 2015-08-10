package com.yazino.platform.service.community;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CashierInformationTest {

    @Test
    public void recognisePurchaseCashier(){
        final YazinoConfiguration config = mock(YazinoConfiguration.class);
        when(config.getList(anyString(), any(List.class))).thenReturn(Arrays.<Object>asList("my cashier", "some_other"));
        final CashierInformation underTest = new CashierInformation(config);
        assertTrue(underTest.isPurchase("my cashier"));
    }
}
