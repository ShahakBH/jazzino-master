package strata.server.lobby.api.promotion.message;

import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TopUpRequestTest {

    @Test
    public void aRequestWithoutANullPlayerIsInvalid(){
        TopUpRequest request = aValidRequest();
        request.setPlayerId(null);
        assertTrue(request.isInvalid());
    }

    @Test
    public void aRequestWithoutANullSessionIsValid(){
        TopUpRequest request = aValidRequest();
        request.setSessionId(null);
        assertFalse(request.isInvalid());
    }

    @Test
    public void aRequestWithoutANullPlatformIsInvalid(){
        TopUpRequest request = aValidRequest();
        request.setPlatform(null);
        assertTrue(request.isInvalid());
    }

    @Test
    public void aRequestWithoutANullTopUpDateIsInvalid(){
        TopUpRequest request = aValidRequest();
        request.setRequestDate(null);
        assertTrue(request.isInvalid());
    }

    private TopUpRequest aValidRequest() {
        return new TopUpRequest(BigDecimal.TEN, Platform.IOS, new DateTime(), BigDecimal.valueOf(666));
    }
}
