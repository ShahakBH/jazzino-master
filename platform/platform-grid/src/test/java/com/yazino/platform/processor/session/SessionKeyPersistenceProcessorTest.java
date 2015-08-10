package com.yazino.platform.processor.session;

import com.yazino.platform.audit.AuditService;
import com.yazino.platform.audit.message.SessionKey;
import com.yazino.platform.model.session.SessionKeyPersistenceRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.mockito.Mockito.*;

public class SessionKeyPersistenceProcessorTest {
    private static final int ACCOUNT_ID = 543;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592L);
    public static final HashMap<String,Object> CLIENT_CONTEXT = new HashMap<String, Object>();

    @Mock
    private AuditService auditService;

    private SessionKeyPersistenceProcessor underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new SessionKeyPersistenceProcessor(auditService);
    }

    @Test
    public void noActionsOccurForANullRequest() {
        underTest.process(null);

        verifyZeroInteractions(auditService);
    }

    @Test
    public void noActionsOccurForANullPayload() {
        underTest.process(aRequestWith(null));

        verifyZeroInteractions(auditService);
    }

    @Test
    public void theSessionKeyIsSentToTheAuditService() {
        underTest.process(aRequestWith(aSessionKey()));

        verify(auditService).auditSessionKey(aSessionKey());
        verifyNoMoreInteractions(auditService);
    }

    @Test
    public void anyExceptionsFromTheAuditServiceAreNotPropagated() {
        doThrow(new RuntimeException("aTestException"))
                .when(auditService).auditSessionKey(aSessionKey());

        underTest.process(aRequestWith(aSessionKey()));

        verify(auditService).auditSessionKey(aSessionKey());
        verifyNoMoreInteractions(auditService);
    }

    @Test(expected = IllegalStateException.class)
    public void anInstanceCreatedWithTheCGLibConstructorThrowAnExceptionOnProcess() {
        new SessionKeyPersistenceProcessor().process(aRequestWith(aSessionKey()));
    }

    private SessionKeyPersistenceRequest aRequestWith(final SessionKey sessionKey) {
        return new SessionKeyPersistenceRequest(sessionKey);
    }

    private SessionKey aSessionKey() {
        return new SessionKey(SESSION_ID, BigDecimal.valueOf(ACCOUNT_ID), BigDecimal.ONE, "keyFor" + ACCOUNT_ID,
                "1.2.3.4", "referrerFor" + ACCOUNT_ID, "platformFor" + ACCOUNT_ID, "theLoginUrl", CLIENT_CONTEXT);
    }

}
