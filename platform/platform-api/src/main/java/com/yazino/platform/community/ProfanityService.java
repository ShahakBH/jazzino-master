package com.yazino.platform.community;

import java.util.Set;

public interface ProfanityService {

    Set<String> findAllProhibitedWords();

    Set<String> findAllProhibitedPartWords();

}
