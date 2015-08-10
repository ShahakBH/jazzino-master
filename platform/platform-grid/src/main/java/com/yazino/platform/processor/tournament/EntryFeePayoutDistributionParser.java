package com.yazino.platform.processor.tournament;

import org.apache.commons.lang3.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Will parse a tab separated payout distribution table.
 * The format should be:
 * <p/>
 * \t<playerRange1>\t<playerRange2\t...\n
 * <positionRange1>\t<payout%>\t<payout%>\t...\n
 * <positionRange2>\t<payout%>\t<payout%>\t...\n
 * <positionRange3>\t<payout%>\t<payout%>\t...
 */
class EntryFeePayoutDistributionParser {
    private final List<Range<Integer>> positionRanges = new ArrayList<Range<Integer>>();
    private final List<EntryFeePrizePoolPayoutStrategy.PlayerRangePayout> payouts
            = new ArrayList<EntryFeePrizePoolPayoutStrategy.PlayerRangePayout>();

    private String distributionTsv =
            "\t2-27\t28-36\t37-45\t46-54\t55-63\t64-72\t73-99\t100-126\t127-153\t154-198\t199-297\t298-396"
                    + "\t397-495\t496-594\t595-693\t694-792\t793-999\t1000-1150\t1151-1400\t1401-1700\t1701-2000\n"
                    + "1\t50.00%\t45.00%\t40.00%\t38.00%\t35.00%\t33.50%\t32.00%\t30.00%\t28.50%\t27.00%\t26.00%"
                    + "\t25.00%\t25.00%\t25.00%\t24.50%\t24.00%\t23.00%\t22.50%\t22.00%\t21.50%\t20.60%\n"
                    + "2\t30.00%\t25.00%\t23.00%\t22.00%\t21.00%\t20.00%\t19.50%\t19.00%\t18.00%\t17.00%\t16.50%"
                    + "\t16.00%\t15.50%\t15.50%\t15.25%\t15.00%\t14.50%\t14.25%\t14.00%\t13.60%\t13.60%\n"
                    + "3\t20.00%\t18.00%\t16.00%\t15.00%\t15.00%\t14.50%\t14.00%\t13.25%\t13.00%\t12.75%"
                    + "\t12.50%\t12.00%\t11.50%\t11.25%\t11.25%\t11.25%\t10.75%\t10.55%\t10.13%\t10.10%\t10.10%\n"
                    + "4\t\t12.00%\t12.00%\t11.00%\t11.00%\t11.00%\t11.00%\t10.50%\t10.25%\t10.00%\t9.75%"
                    + "\t9.25%\t9.00%\t8.75%\t8.75%\t8.50%\t8.25%\t8.25%\t8.13%\t8.00%\t7.90%\n"
                    + "5\t\t\t9.00%\t8.00%\t8.00%\t8.00%\t8.00%\t7.50%\t7.50%\t7.50%\t7.25%\t7.00%\t6.75%"
                    + "\t6.50%\t6.50%\t6.50%\t6.25%\t6.25%\t6.13%\t6.00%\t5.90%\n"
                    + "6\t\t\t\t6.00%\t6.00%\t6.00%\t6.00%\t5.50%\t5.50%\t5.50%\t5.25%\t5.00%\t5.00%\t4.75%"
                    + "\t4.75%\t4.50%\t4.50%\t4.50%\t4.50%\t4.30%\t4.10%\n"
                    + "7\t\t\t\t\t4.00%\t4.00%\t4.00%\t3.75%\t3.75%\t3.75%\t3.50%\t3.25%\t3.25%\t3.10%\t3.10%"
                    + "\t3.10%\t3.00%\t3.00%\t2.90%\t2.80%\t2.50%\n"
                    + "8\t\t\t\t\t\t3.00%\t3.00%\t3.00%\t3.00%\t3.00%\t2.75%\t2.50%\t2.50%\t2.50%\t2.40%\t2.40%"
                    + "\t2.34%\t2.25%\t2.10%\t2.00%\t1.70%\n"
                    + "9\t\t\t\t\t\t\t2.50%\t2.25%\t2.25%\t2.25%\t2.10%\t2.00%\t2.00%\t1.95%\t1.90%\t1.80%\t1.70%"
                    + "\t1.60%\t1.50%\t1.40%\t1.20%\n"
                    + "12\t\t\t\t\t\t\t\t1.75%\t1.50%\t1.50%\t1.25%\t1.25%\t1.15%\t1.10%\t1.05%\t1.00%\t0.95%"
                    + "\t0.90%\t0.85%\t0.84%\t0.80%\n"
                    + "15\t\t\t\t\t\t\t\t\t1.25%\t1.25%\t1.00%\t1.00%\t0.90%\t0.85%\t0.80%\t0.75%\t0.70%\t0.66%"
                    + "\t0.62%\t0.60%\t0.55%\n"
                    + "18\t\t\t\t\t\t\t\t\t\t1.00%\t0.75%\t0.75%\t0.70%\t0.60%\t0.55%\t0.50%\t0.50%\t0.46%\t0.44%"
                    + "\t0.40%\t0.39%\n"
                    + "27\t\t\t\t\t\t\t\t\t\t\t0.60%\t0.55%\t0.50%\t0.45%\t0.43%\t0.40%\t0.39%\t0.37%\t0.35%"
                    + "\t0.33%\t0.32%\n"
                    + "36\t\t\t\t\t\t\t\t\t\t\t\t0.45%\t0.40%\t0.38%\t0.35%\t0.35%\t0.34%\t0.32%\t0.30%\t0.28%\t0.27%\n"
                    + "45\t\t\t\t\t\t\t\t\t\t\t\t\t0.35%\t0.33%\t0.30%\t0.30%\t0.30%\t0.28%\t0.26%\t0.24%\t0.24%\n"
                    + "54\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.30%\t0.28%\t0.28%\t0.27%\t0.25%\t0.23%\t0.21%\t0.21%\n"
                    + "63\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.25%\t0.25%\t0.24%\t0.22%\t0.21%\t0.19%\t0.19%\n"
                    + "72\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.23%\t0.22%\t0.20%\t0.19%\t0.17%\t0.17%\n"
                    + "81\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.20%\t0.18%\t0.17%\t0.16%\t0.16%\n"
                    + "90\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.18%\t0.17%\t0.16%\t0.15%\t0.15%\n"
                    + "99\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.17%\t0.15%\t0.15%\t0.14%\n"
                    + "108\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.16%\t0.15%\t0.14%\t0.14%\n"
                    + "117\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.14%\t0.14%\t0.13%\n"
                    + "126\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.14%\t0.13%\t0.13%\n"
                    + "135\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.13%\t0.13%\t0.12%\n"
                    + "162\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.12%\t0.12%\n"
                    + "189\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0.11%";

    public void parse() {
        reset();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new StringReader(distributionTsv));
            String line = reader.readLine();
            parsePlayerRanges(line);

            final List<String> positionRangesValues = new ArrayList<String>();

            int numberLines = 0;
            while ((line = reader.readLine()) != null) {
                numberLines++;
                final String[] columns = line.split("\t");
                final String positionRange = columns[0];
                if (positionRange.trim().length() > 0) {
                    positionRangesValues.add(positionRange);
                }
                for (int i = 1; i < columns.length; i++) {
                    final String payout = columns[i];
                    if (payout.trim().length() > 0) {
                        payouts.get(i - 1).addPayout(payout.replaceAll("%", ""));
                    }
                }
            }

            fillInPositionRanges(positionRangesValues);

            if (positionRanges.size() != numberLines) {
                throw new IllegalStateException(String.format("Failed to parse correct number of position ranges,"
                        + " expected [%s], actual [%s]", numberLines, positionRanges.size()));
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse static string", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void fillInPositionRanges(final List<String> positionRangeValues) {
        final List<Range<Integer>> ranges = new ArrayList<Range<Integer>>();
        for (int i = positionRangeValues.size() - 1; i >= 0; i--) {
            final int end = Integer.parseInt(positionRangeValues.get(i));
            int start = end;
            if (i - 1 >= 0) {
                start = Integer.parseInt(positionRangeValues.get(i - 1)) + 1;
            }
            ranges.add(Range.between(start, end));
        }
        Collections.reverse(ranges);
        positionRanges.addAll(ranges);
    }

    private void parsePlayerRanges(final String playerRangesLine) {
        final String[] playerRangesValues = playerRangesLine.split("\t");
        for (String rangeValue : playerRangesValues) {
            if (rangeValue.trim().length() > 0) {
                final String[] bounds = rangeValue.split("-");
                final Range<Integer> range = Range.between(Integer.parseInt(bounds[0]), Integer.parseInt(bounds[1]));
                payouts.add(new EntryFeePrizePoolPayoutStrategy.PlayerRangePayout(range));
            }
        }
    }

    private void reset() {
        positionRanges.clear();
        payouts.clear();
    }

    @SuppressWarnings("unchecked")
    public Range<Integer>[] getPositionRanges() {
        return positionRanges.toArray(new Range[positionRanges.size()]);
    }

    public List<EntryFeePrizePoolPayoutStrategy.PlayerRangePayout> getPayouts() {
        return payouts;
    }

    public void setDistributionTsv(final String distributionTsv) {
        this.distributionTsv = distributionTsv;
    }
}
