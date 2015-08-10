package com.yazino.platform.processor.statistic.opengraph;

import com.yazino.game.api.facebook.OpenGraphActionProvider;
import com.yazino.platform.opengraph.OpenGraphAction;
import com.yazino.platform.opengraph.OpenGraphObject;
import com.yazino.platform.playerstatistic.StatisticEvent;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DefaultStatisticToActionTransformerTest {

    private DefaultStatisticToActionTransformer underTest;

    @Before
    public void setUp() {
        underTest = new DefaultStatisticToActionTransformer();
    }

    @Test
    public void aMatchedEventGeneratedAnAction() {
        underTest.registerActions(new OpenGraphActionProvider(
                new com.yazino.game.api.facebook.OpenGraphAction("AN_ACHIEVEMENT_OF_SORTS", "anAction", "anObject", "anId")));

        final OpenGraphAction result = underTest.apply(new StatisticEvent("AN_ACHIEVEMENT_OF_SORTS"));

        assertThat(result, is(equalTo(new OpenGraphAction("anAction", new OpenGraphObject("anObject", "anId")))));
    }

    @Test
    public void anUnregisteredActionIsNoLongerMatched() {
        final OpenGraphActionProvider actionProvider = new OpenGraphActionProvider(
                new com.yazino.game.api.facebook.OpenGraphAction("AN_ACHIEVEMENT_OF_SORTS", "anAction", "anObject", "anId"));
        underTest.registerActions(actionProvider);

        assertThat(underTest.apply(new StatisticEvent("AN_ACHIEVEMENT_OF_SORTS")), is(not(nullValue())));

        underTest.unregisterActions(actionProvider);

        assertThat(underTest.apply(new StatisticEvent("AN_ACHIEVEMENT_OF_SORTS")), is(nullValue()));
    }

    @Test
    public void anUnmatchedEventDoesNotGenerateAnAction() {
        final OpenGraphAction result = underTest.apply(new StatisticEvent("AN_ACHIEVEMENT_OF_SORTS"));

        assertThat(result, is(nullValue()));
    }

}
