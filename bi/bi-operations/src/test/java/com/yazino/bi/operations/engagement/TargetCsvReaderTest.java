package com.yazino.bi.operations.engagement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.bi.operations.util.TargetCsvReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class TargetCsvReaderTest {

    @Mock
    private InputStream inputStream;

    private TargetCsvReader underTest;

    private final String SLOTS = "SLOTS";
    private final String BLACKJACK = "BLACKJACK";
    private final String ID1 = "1";
    private final String ID2 = "2";
    private final String EXTERNAL_ID1 = "101";
    private final String EXTERNAL_ID2 = "202";

    public TargetCsvReaderTest() {
        underTest = new TargetCsvReader();
    }

    @Test
    public void shouldReadTargetCsv() throws IOException {
        final String sourceList = buildCsvInputString();


        buildExpectedTargets();

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        //when reading input file
        final Set<AppRequestTarget> targets = underTest.readTargets(inputStream);

        // should have 4 targets
        assertTrue(targets.size() == 4);
        assertThat(targets, hasItems(buildExpectedTargets()));
    }

    private AppRequestTarget[]  buildExpectedTargets() {
        return new AppRequestTarget[] {new AppRequestTargetBuilder()
                .withGameType(SLOTS).withPlayerId(new BigDecimal(ID1)).build(),
                new AppRequestTargetBuilder()
                        .withGameType(BLACKJACK).withPlayerId(new BigDecimal(ID2)).build(),
                new AppRequestTargetBuilder()
                        .withGameType(SLOTS).withPlayerId(new BigDecimal(ID1)).withExternalId(EXTERNAL_ID1).build(),
                new AppRequestTargetBuilder()
                        .withGameType(SLOTS).withPlayerId(new BigDecimal(ID2)).build()};
    }

    private String buildCsvInputString() {
        // invalid since only 1 column
        final String line1 = "hsfjdk";
        final String line2 = " , ";
        final String line3 = "SLOTS";
        // invalid since playerid is null
        final String line4 = "SLOTS,";
        // valid with no external id
        final String line5 = SLOTS + "," + ID1;
        // invalid - it's empty
        final String line5a = "";
        final String line6 = BLACKJACK + "   ,   " + ID2;
        // invalid since player id must be an integer
        final String line7 = "BLACKJACK,XXX";
        // valid with external id
        final String line9 = SLOTS + "," + ID1 + "," + EXTERNAL_ID1;
        // valid with empty external id
        final String line10 = SLOTS + "," + ID2 + ",";

        return line1 + "\n"
                + line2 + "\n"
                + line3 + "\n"
                + line4 + "\n"
                + line5 + "\n"
                + line5a + "\n"
                + line6 + "\n"
                + line7 + "\n"
                + line9 + "\n"
                + line10 + "\n";
    }
}