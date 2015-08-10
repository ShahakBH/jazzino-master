package strata.server.lobby.promotion.tools;

import strata.server.lobby.api.promotion.Promotion;

import java.util.Random;

public class PromotionFunctions {

    private Random random = new Random();

    public void setRandom(final Random random) {
        this.random = random;
    }

    public int generateSeed() {
        return random.nextInt(Promotion.MAX_SEED_VALUE + 1);
    }

}
