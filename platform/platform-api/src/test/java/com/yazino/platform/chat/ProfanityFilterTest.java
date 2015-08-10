package com.yazino.platform.chat;

import com.yazino.platform.community.ProfanityFilter;
import com.yazino.platform.community.ProfanityService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class ProfanityFilterTest {

    @Mock
    private ProfanityService profanityService;

    private ProfanityFilter underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(profanityService.findAllProhibitedWords()).thenReturn(newHashSet("fullWord1", "fullWord2"));
        when(profanityService.findAllProhibitedPartWords()).thenReturn(newHashSet("partWord1", "partWord2"));

        underTest = new ProfanityFilter(profanityService);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void shouldNotAcceptNullString() {
        underTest.filter(null);
    }
    
    @Test
    public void forbiddenWordsShouldBeBlockedByTheFilter() {
        assertThat(underTest.filter("a string with fullWord1 in it"), is(equalTo("a string with ********* in it")));
    }

    @Test
    public void forbiddenPartWordsShouldBeBlockedByTheFilter() {
        assertThat(underTest.filter("a string with aWordWithpartWord1Somewhere in it"),
                is(equalTo("a string with aWordWith*********Somewhere in it")));
    }

    @Test
    public void multipleForbiddenWordsShouldBeBlockedByTheFilter() {
        assertThat(underTest.filter("a string with fullWord1 and fullWord2 in it"),
                is(equalTo("a string with ********* and ********* in it")));
    }

    @Test
    public void multipleForbiddenPartWordsShouldBeBlockedByTheFilter() {
        assertThat(underTest.filter("a string with aWordWithpartWord1Somewhere and anotherWordWithpartWord2 in it"),
                is(equalTo("a string with aWordWith*********Somewhere and anotherWordWith********* in it")));
    }

    @Test
    public void forbiddenWordsAreIgnoredWhenSubstrings() {
        assertThat(underTest.filter("a string with fullWord1AsPartOfAnotherWord and aCopyOffullWord2AsWell in it"),
                is(equalTo("a string with fullWord1AsPartOfAnotherWord and aCopyOffullWord2AsWell in it")));
    }

    @Test
    public void forbiddenPartWordsAreBlockedWhenFullWords() {
        assertThat(underTest.filter("a string with partWord1 in it"), is(equalTo("a string with ********* in it")));
    }

    @Test
    public void forbiddenWordsAndPartWordsAreBlocked() {
        assertThat(underTest.filter("a string with fullWord1 and partWord1InAWord and apartWord2AsWell in it"),
                is(equalTo("a string with ********* and *********InAWord and a*********AsWell in it")));
    }
}
