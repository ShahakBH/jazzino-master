package com.yazino.platform.test;

import com.yazino.platform.community.ProfanityService;

import java.util.HashSet;
import java.util.Set;

public class DummyProfanityService implements ProfanityService {
    private final Set<String> badWords = new HashSet<String>();
    private final Set<String> partBadWords = new HashSet<String>();

    @Override
    public Set<String> findAllProhibitedWords() {
        return getBadWords();
    }

    @Override
    public Set<String> findAllProhibitedPartWords() {
        return getPartBadWords();
    }

    public Set<String> getBadWords() {
        return badWords;
    }

    public Set<String> getPartBadWords() {
        return partBadWords;
    }

    public void addBarWord(final String badWord) {
        badWords.add(badWord);
    }
}
