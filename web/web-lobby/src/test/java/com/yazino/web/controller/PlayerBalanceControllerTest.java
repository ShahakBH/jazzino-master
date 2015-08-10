package com.yazino.web.controller;

import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import static org.mockito.Mockito.*;

public class PlayerBalanceControllerTest {

    private PlayerService playerService;
    private WalletService walletService;
    private HttpServletResponse response;
    private PrintWriter writer;

    private PlayerBalanceController underTest;

    @Before
    public void setUp() throws Exception {
        playerService = mock(PlayerService.class);
        walletService = mock(WalletService.class);
        response = mock(HttpServletResponse.class);
        writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
        underTest = new PlayerBalanceController(playerService, walletService);
    }

    @Test
    public void shouldIgnoreIfPlayerIdNotPresent() throws IOException {
        underTest.balance(null, response);
        String balance = "0";
        verify(writer).write("{\"balance\": " + balance + "}");

        verifyZeroInteractions(playerService);
        verifyZeroInteractions(walletService);
    }

    @Test
    public void shouldIgnoreIfPlayerIdEmpty() throws IOException {
        underTest.balance("", response);
        String balance = "0";
        verify(writer).write("{\"balance\": " + balance + "}");

        verifyZeroInteractions(playerService);
        verifyZeroInteractions(walletService);
    }

    @Test
    public void shouldIgnoreIfPlayerIdInvalid() throws IOException {
        underTest.balance("someString", response);
        String balance = "0";
        verify(writer).write("{\"balance\": " + balance + "}");

        verifyZeroInteractions(playerService);
        verifyZeroInteractions(walletService);
    }
    
    @Test
    public void shouldIgnoreIfRequestFails() throws IOException {
        when(playerService.getAccountId(BigDecimal.valueOf(1))).thenThrow(new RuntimeException("something nasty"));
        underTest.balance("1", response);
        String balance = "0";
        verify(writer).write("{\"balance\": " + balance + "}");
    }
    
    @Test
    public void shouldRetrieveBalance() throws IOException, WalletServiceException {
        BigDecimal balance = BigDecimal.TEN;
        when(playerService.getAccountId(BigDecimal.ONE)).thenReturn(BigDecimal.TEN);
        when(walletService.getBalance(BigDecimal.TEN)).thenReturn(balance);
        underTest.balance("1", response);
        verify(writer).write("{\"balance\": " + balance + "}");
    }
}
