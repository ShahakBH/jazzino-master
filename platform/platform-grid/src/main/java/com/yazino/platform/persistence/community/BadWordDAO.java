package com.yazino.platform.persistence.community;

import java.util.Set;

public interface BadWordDAO {
    Set<String> findAllBadWords();

    Set<String> findAllPartBadWords();
}
