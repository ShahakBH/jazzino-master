package strata.server.lobby.api.promotion.util;

import strata.server.lobby.api.promotion.Promotion;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator based on highest priority then earliest start date
 */
public class PromotionPriorityDateComparator implements Comparator<Promotion>, Serializable {
    private static final long serialVersionUID = -5889289305259166370L;

    @Override
    public int compare(final Promotion p1, final Promotion p2) {
        int p1Priority = 0;
        if (p1.getPriority() != null) {
            p1Priority = p1.getPriority();
        }
        int p2Priority = 0;
        if (p2.getPriority() != null) {
            p2Priority = p2.getPriority();
        }
        final int priorityDiff = p2Priority - p1Priority;
        if (priorityDiff == 0) {
            return p1.getStartDate().compareTo(p2.getStartDate());
        }
        return priorityDiff;
    }
}
