package com.yazino.platform.gifting;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PlayerCollectionStatusTest {

    @Test
    public void collectionsThatCanBeMadeImmediatelyShouldReturnLesserOfRemainingForCurrentPeriodAndWaitingToBeCollected() {
        assertThatCollectionsThatCanBeMadeImmediately(quota(25), gifts(10), expected(10)); // quota exceeds gifts
        assertThatCollectionsThatCanBeMadeImmediately(quota(10), gifts(25), expected(10)); // gifts exceeds quota
        assertThatCollectionsThatCanBeMadeImmediately(quota(10), gifts(10), expected(10)); // gifts equals quota
    }

    private int quota(final int value) {
        return value;
    }

    private int gifts(final int value) {
        return value;
    }

    private int expected(final int value) {
        return value;
    }

    private void assertThatCollectionsThatCanBeMadeImmediately(int collectionsRemainingForCurrentPeriod,
                                                               int giftsWaitingToBeCollected,
                                                               int expected) {
        PlayerCollectionStatus underTest = new PlayerCollectionStatus(collectionsRemainingForCurrentPeriod, giftsWaitingToBeCollected);
        assertThat(new PlayerCollectionStatus(collectionsRemainingForCurrentPeriod, giftsWaitingToBeCollected).collectionsThatCanBeMadeImmediately(), equalTo(expected));
    }
}
