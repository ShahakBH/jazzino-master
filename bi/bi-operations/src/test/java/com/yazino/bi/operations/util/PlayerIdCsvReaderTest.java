package com.yazino.bi.operations.util;

import com.yazino.logging.appender.ListAppender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.bi.operations.persistence.PlayerInformationDao;
import com.yazino.bi.operations.persistence.TooManyPlayersMatchedToExternalIdException;
import com.yazino.bi.operations.model.PlayerReaderResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class PlayerIdCsvReaderTest {
    @Mock
    private InputStream inputStream;

    @Mock
    private PlayerInformationDao dao;

    private PlayerIdCsvReader underTest;

    @Before
    public void init() {
        underTest = new PlayerIdCsvReader(dao);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWithNullStream() throws IOException {
        underTest.readPlayerIds(null);
    }

    @Test
    public void shouldReadPlayerInformationAndSendBackPlayerIds() throws IOException {
        // GIVEN 2 lines are read from the CSV file, 1 external and one internal
        final String rpx1 = "GOOGLE";
        final String rpx2 = "YAZINO";
        final String id1 = "abc";
        final String id2 = "123";
        // and 1 is a player id
        final String sourceList = rpx1 + "," + id1 + "\n" + rpx2 + "," + id2;
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        // AND the DAO returns the matching player IDs
        final BigDecimal playerId1 = BigDecimal.ONE;
        given(dao.getPlayerId(rpx1, id1)).willReturn(playerId1);
        final BigDecimal playerId2 = BigDecimal.TEN;
        given(dao.getPlayerId(rpx2, id2)).willReturn(playerId2);

        // WHEN requesting the player IDs
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        // THEN the list of two matching IDs is returned
        assertThat(playerReaderResult.getPlayerIds(), hasItems(playerId1, playerId2));
        // AND all external ids were matched exactly once
        assertFalse(playerReaderResult.hasRpxErrors());
    }

    @Test
    public void shouldTrimWhitespaceWhenReadingPlayerInformationAndSendBackPlayerIds() throws IOException {
        // GIVEN two lines are read from the CSV file
        final String rpx1 = "GOOGLE";
        final String rpx1WithWS = " " + rpx1 + "\t";
        final String rpx2 = "YAZINO";
        final String rpx2WithWS = "\t" + rpx2 + " ";
        final String id1 = "abc";
        final String id1WithWS = "\t" + id1 + " ";
        final String id2 = "123";
        final String id2WithWS = " " + id2 + "\t";
        final String sourceList = rpx1WithWS + "," + id1WithWS + "\n" + rpx2WithWS + "," + id2WithWS;
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        // AND the DAO returns the matching player IDs
        final BigDecimal playerId1 = BigDecimal.ONE;
        given(dao.getPlayerId(rpx1, id1)).willReturn(playerId1);
        final BigDecimal playerId2 = BigDecimal.TEN;
        given(dao.getPlayerId(rpx2, id2)).willReturn(playerId2);

        // WHEN requesting the player IDs
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        // THEN the list of two matching IDs is returned
        assertThat(playerReaderResult.getPlayerIds(), hasItems(playerId1, playerId2));
        // AND all external ids were matched exactly once
        assertFalse(playerReaderResult.hasRpxErrors());
    }

    @Test
    public void shouldRpxCredentialToUnmatchedListWhenNullReturnedForPlayerId() throws IOException {
        // GIVEN two lines are read from the CSV file
        final String rpx1 = "GOOGLE";
        final String rpx2 = "YAZINO";
        final String id1 = "abc";
        final String id2 = "123";
        final String sourceList = rpx1 + "," + id1 + "\n" + rpx2 + "," + id2;
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        // AND the DAO returns the matching player IDs
        final BigDecimal playerId1 = BigDecimal.ONE;
        given(dao.getPlayerId(rpx1, id1)).willReturn(playerId1);
        final BigDecimal playerId2 = BigDecimal.TEN;
        given(dao.getPlayerId(rpx2, id2)).willReturn(null);

        // WHEN requesting the player IDs
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        assertEquals(1, playerReaderResult.getPlayerIds().size());
        assertThat(playerReaderResult.getPlayerIds(), hasItem(playerId1));
        assertTrue(playerReaderResult.hasRpxErrors());
        assertThat(playerReaderResult.getNotMatched(), hasItem(new PlayerReaderResult.RpxCredential(rpx2, id2)));
    }

    @Test
    public void shouldAddRpxCredentialWithPlayerIdsToResultWhenMutliplePlayerIdsAreMatchedToSingleRpxId() throws IOException {
        // GIVEN two lines are read from the CSV file
        final String rpx1 = "GOOGLE";
        final String rpx2 = "YAZINO";
        final String id1 = "abc";
        final String id2 = "123";
        final String sourceList = rpx1 + "," + id1 + "\n" + rpx2 + "," + id2;
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        // AND the DAO returns the matching player IDs
        final BigDecimal playerId1 = BigDecimal.ONE;
        final BigDecimal playerId2 = BigDecimal.valueOf(2);
        given(dao.getPlayerId(rpx1, id1)).willThrow(new TooManyPlayersMatchedToExternalIdException(rpx1, id1, Arrays.asList(playerId1, playerId2)));
        final BigDecimal playerId3 = BigDecimal.TEN;
        final BigDecimal playerId4 = BigDecimal.valueOf(12);
        given(dao.getPlayerId(rpx2, id2)).willThrow(new TooManyPlayersMatchedToExternalIdException(rpx2, id2, Arrays.asList(playerId3, playerId4)));

        // WHEN requesting the player IDs
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        assertEquals(0, playerReaderResult.getPlayerIds().size());
        assertTrue(playerReaderResult.hasRpxErrors());
        assertTrue(playerReaderResult.getNotMatched().isEmpty());
        assertThat(playerReaderResult.getMultipleMatches(), hasItems(new PlayerReaderResult.RpxCredential(rpx1, id1, Arrays.asList(playerId1, playerId2)),
                new PlayerReaderResult.RpxCredential(rpx2, id2, Arrays.asList(playerId3, playerId4))));
    }

    @Test
    public void shouldLogWarningOnMissingPlayerId() throws IOException {
        // GIVEN two lines are read from the CSV file
        final String rpx1 = "GOOGLE";
        final String rpx2 = "YAZINO";
        final String id1 = "abc";
        final String id2 = "123";
        final String sourceList = rpx1 + "," + id1 + "\n" + rpx2 + "," + id2;
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        // AND we watch the log appender for the given class
        final ListAppender fallbackAppender = ListAppender.addTo(PlayerIdCsvReader.class);

        // AND the DAO returns the matching player IDs, except for the first one
        final BigDecimal playerId1 = BigDecimal.ONE;
        given(dao.getPlayerId(rpx1, id1)).willReturn(null);
        final BigDecimal playerId2 = BigDecimal.TEN;
        given(dao.getPlayerId(rpx2, id2)).willReturn(playerId2);

        // WHEN requesting the player IDs
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        // THEN the list containing a correct player ID is returned
        assertEquals(1, playerReaderResult.getPlayerIds().size());
        assertThat(playerReaderResult.getPlayerIds(), hasItems(playerId2));

        // AND the logger contains a warning about the missing player
        assertThat((Iterable<String>) fallbackAppender.getMessages(),
                hasItem("No existing player in the database for GOOGLE/abc"));
    }

    @Test
    public void shouldAddInvalidLineToResultWithMalFormedLine() throws IOException {
        // GIVEN 3 lines are read from the CSV file, the first being malformed
        final String line1Part1 = "111111";
        final String line1Part2 = "2222222";
        final String line1Part3 = "3333333";
        final String rpx1 = "GOOGLE";
        final String rpx2 = "YAZINO";
        final String id1 = "abc";
        final String id2 = "123";
        final String sourceList = line1Part1 + "," + line1Part2 + "," + line1Part3 + "\n" + rpx1 + "," + id1 + "\n" + rpx2 + "," + id2;
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        // AND the DAO returns the matching player IDs
        final BigDecimal playerId1 = BigDecimal.ONE;
        given(dao.getPlayerId(rpx1, id1)).willReturn(playerId1);
        final BigDecimal playerId2 = BigDecimal.TEN;
        given(dao.getPlayerId(rpx2, id2)).willReturn(playerId2);

        // WHEN requesting the player IDs
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        // THEN the list of two matching IDs is returned
        assertThat(playerReaderResult.getPlayerIds(), hasItems(playerId1, playerId2));
        // AND all external ids were matched exactly once
        assertFalse(playerReaderResult.hasRpxErrors());
        // and single invalid input line
        assertEquals(1, playerReaderResult.getInvalidInputLines().size());
        assertThat(playerReaderResult.getInvalidInputLines().get(0), hasItems(line1Part1, line1Part2, line1Part3));
    }


    @Test
    public void shouldAddInvalidLineToResultWhenSingleLineValueIsNotAnInteger() throws IOException {
        // GIVEN the following input stream
        final String id1 = "abc";
        final String id2 = "10.2";
        final String sourceList = id1 + "\n" + id2;
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        // WHEN requesting the player IDs
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        // THEN invalid player id should be added to invalid lines
        assertTrue(playerReaderResult.getPlayerIds().isEmpty());
        assertFalse(playerReaderResult.hasRpxErrors());
        assertEquals(2, playerReaderResult.getInvalidInputLines().size());
        assertThat(playerReaderResult.getInvalidInputLines().get(0), hasItem(id1));
        assertThat(playerReaderResult.getInvalidInputLines().get(1), hasItem(id2));
    }

    @Test
    public void shouldReadEmptyStream() throws IOException {
        // GIVEN the following input stream
        final String x = "";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(x.getBytes());

        // WHEN reading the player ids
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        // THEN players should be empty
        assertTrue(playerReaderResult.getPlayerIds().isEmpty());
    }

    @Test
    public void shouldReadPlainIdsCsvFileAndSendBackPlayerIds() throws IOException {
        // GIVEN the following input stream
        final String id1 = "123";
        final String id2 = "234";
        final String id3 = "345";
        final String sourceList = id1 + "\n" + id2 + "\n" + id3;
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceList.getBytes());

        // WHEN requesting the player IDs
        final PlayerReaderResult playerReaderResult = underTest.readPlayerIds(inputStream);

        // THEN the list of two matching IDs is returned
        assertThat(playerReaderResult.getPlayerIds(), hasItems(BigDecimal.valueOf(123), BigDecimal.valueOf(234), BigDecimal.valueOf(345)));

        // AND there are no interactions with the DAO
        verifyNoMoreInteractions(dao);
    }
}
