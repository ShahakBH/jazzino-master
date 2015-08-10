package com.yazino.platform.service.community;

import com.yazino.platform.persistence.community.BadWordDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class GigaspaceRemotingProfanityServiceTest {
    @Mock
    private BadWordDAO badWordDAO;

    private GigaspaceRemotingProfanityService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new GigaspaceRemotingProfanityService(badWordDAO);
    }

    @Test(expected = NullPointerException.class)
    public void aNullBadWordRepositoryIsRejected() {
        new GigaspaceRemotingProfanityService(null);
    }

    @Test
    public void findingAllProhibitedWordsDelegatesToTheRepository() {
        when(badWordDAO.findAllBadWords()).thenReturn(newHashSet("word1", "word2", "word3"));

        final Set<String> allWords = underTest.findAllProhibitedWords();

        assertThat(allWords, is(equalTo((Set<String>) newHashSet("word1", "word2", "word3"))));
    }

    @Test
    public void findingAllProhibitedPartWordsDelegatesToTheRepository() {
        when(badWordDAO.findAllPartBadWords()).thenReturn(newHashSet("part1", "part2", "part3"));

        final Set<String> allPartWords = underTest.findAllProhibitedPartWords();

        assertThat(allPartWords, is(equalTo((Set<String>) newHashSet("part1", "part2", "part3"))));
    }

}
