package strata.server.lobby.api.promotion;

import java.io.Serializable;

public enum ProgressiveAwardEnum implements Serializable {
    AWARD_1("PROGRESSIVE_DAY_1", 0),
    AWARD_2("PROGRESSIVE_DAY_2", 1),
    AWARD_3("PROGRESSIVE_DAY_3", 2),
    AWARD_4("PROGRESSIVE_DAY_4", 3),
    AWARD_5("PROGRESSIVE_DAY_5", 4);

    private static final long serialVersionUID = 2L;

    private final String name;
    private final int consecutiveDaysPlayed;


    ProgressiveAwardEnum(final String name, final int consecutiveDaysPlayed) {
        this.name = name;
        this.consecutiveDaysPlayed = consecutiveDaysPlayed;
    }

    public String getName() {
        return name;
    }

    public int getConsecutiveDaysPlayed() {
        return consecutiveDaysPlayed;
    }

    public ProgressiveAwardEnum getNext() {
        if (this.ordinal() < ProgressiveAwardEnum.values().length - 1) {
            return ProgressiveAwardEnum.values()[this.ordinal() + 1];
        } else {
            return AWARD_5;
        }
    }

    public static ProgressiveAwardEnum getProgressiveAwardEnumForConsecutiveDaysPlayed(final int days) {
        for (ProgressiveAwardEnum award: ProgressiveAwardEnum.values()) {
            if (award.getConsecutiveDaysPlayed() == days) {
                return  award;
            }
        }
        return AWARD_1;
    }



    public int getDailyAwardNumber() {
        return consecutiveDaysPlayed + 1;
    }

    public static ProgressiveAwardEnum valueOf(final PromotionType progressivePromotionType) {
        switch (progressivePromotionType) {
            case PROGRESSIVE_DAY_1:
                return AWARD_1;
            case PROGRESSIVE_DAY_2:
                return AWARD_2;
            case PROGRESSIVE_DAY_3:
                return AWARD_3;
            case PROGRESSIVE_DAY_4:
                return AWARD_4;
            case PROGRESSIVE_DAY_5:
                return AWARD_5;
            default:
                throw new IllegalArgumentException(
                        String.format("Cannot map from %s to ProgressiveAwardEnum", progressivePromotionType.name()));
        }
    }
}
