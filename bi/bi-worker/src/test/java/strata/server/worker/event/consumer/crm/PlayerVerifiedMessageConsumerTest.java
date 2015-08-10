package strata.server.worker.event.consumer.crm;

import com.yazino.platform.worker.message.PlayerVerifiedMessage;
import com.yazino.platform.worker.message.VerificationType;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

@RunWith(MockitoJUnitRunner.class)
public class PlayerVerifiedMessageConsumerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(200);

    private PlayerVerifiedMessageConsumer underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(23342);
        underTest = new PlayerVerifiedMessageConsumer();
    }

    @Test
    public void aNullMessageIsSilentlyDropped() {
        underTest.handle(null);
    }

    @Test
    public void aMessageIsIgnored() {
        underTest.handle(new PlayerVerifiedMessage(PLAYER_ID, VerificationType.PLAYED));
    }

}
