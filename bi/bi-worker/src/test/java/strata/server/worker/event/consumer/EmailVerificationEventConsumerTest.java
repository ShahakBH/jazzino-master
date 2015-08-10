package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.EmailValidationEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresEmailValidationDWDAO;

import java.math.BigDecimal;

import static com.google.common.collect.Lists.newArrayList;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailVerificationEventConsumerTest {

    @Mock
    private PostgresEmailValidationDWDAO pgDao;
    @Mock
    private YazinoConfiguration configuration;

    private EmailValidationEventConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new EmailValidationEventConsumer(configuration, pgDao);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void daoIsCommitAware() {
        assertThat(underTest instanceof CommitAware, is(true));
    }

    @Test
    public void shouldSaveUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);
        final EmailValidationEvent message = aMessageForEmailValidation(BigDecimal.ONE);

        underTest.handle(message);
        underTest.consumerCommitting();

        verify(pgDao).saveAll(newArrayList(message));
    }

    @Test
    public void shouldSaveMultipleMessagesOnCommitUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessageForEmailValidation(valueOf(1)));
        underTest.handle(aMessageForEmailValidation(valueOf(2)));
        underTest.handle(aMessageForEmailValidation(valueOf(3)));
        underTest.consumerCommitting();

        verify(pgDao).saveAll(newArrayList(aMessageForEmailValidation(valueOf(1)),
                aMessageForEmailValidation(valueOf(3)),
                aMessageForEmailValidation(valueOf(2))));
    }

    @Test
    public void shouldFlushMessageOnCommitUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessageForEmailValidation(valueOf(1)));
        underTest.handle(aMessageForEmailValidation(valueOf(2)));
        underTest.handle(aMessageForEmailValidation(valueOf(3)));
        underTest.consumerCommitting();

        verify(pgDao).saveAll(newArrayList(aMessageForEmailValidation(valueOf(1)),
                aMessageForEmailValidation(valueOf(3)),
                aMessageForEmailValidation(valueOf(2))));

        underTest.handle(aMessageForEmailValidation(valueOf(4)));
        underTest.handle(aMessageForEmailValidation(valueOf(5)));
        underTest.consumerCommitting();

        verify(pgDao).saveAll(newArrayList(aMessageForEmailValidation(valueOf(5)),aMessageForEmailValidation(valueOf(4))));
    }

    @Test
    public void shouldDisableSaveUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);
        final EmailValidationEvent message = aMessageForEmailValidation(BigDecimal.ONE);

        underTest.handle(message);
        underTest.consumerCommitting();

        verifyZeroInteractions(pgDao);
    }

    private EmailValidationEvent aMessageForEmailValidation(final BigDecimal emailAddressNumber) {
        return new EmailValidationEvent("emailAddress"+emailAddressNumber.toString(), "V");
    }
}
