package com.yazino.game.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

public interface Randomizer extends Serializable {

    void reset();

    int nextInt();

    int nextInt(int n);

    int nextInt(int minInclusive, int maxExclusive);

    Set<Integer> getRandomNumbers(int min, int max, int numberOfRandomsToGet);

    Object getRandomItemFromCollection(Collection collecton);
}
