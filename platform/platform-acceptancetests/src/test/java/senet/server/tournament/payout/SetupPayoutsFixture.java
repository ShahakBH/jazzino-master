package senet.server.tournament.payout;

import com.yazino.platform.tournament.TournamentVariationPayout;
import fitlibrary.SetUpFixture;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SetupPayoutsFixture extends SetUpFixture {
	private List<TournamentVariationPayout> payouts = new ArrayList<TournamentVariationPayout>();
	private List<BigDecimal> payoutAmounts = new ArrayList<BigDecimal>();

	public void rankPayout(int rank, String payoutAmount) {
		payouts.add(new TournamentVariationPayout(rank, new BigDecimal(payoutAmount)));
	}

	public void rankPayoutAmount(int rank, String payoutAmount) {
		payoutAmounts.add(new BigDecimal(payoutAmount));
	}

	public List<TournamentVariationPayout> getPayouts() {
		return payouts;
	}

	public BigDecimal calculateTotalPrize() {
		BigDecimal result = BigDecimal.ZERO;
		for (BigDecimal amount : payoutAmounts) {
			result = result.add(amount);
		}
		return result;
	}

	public List<TournamentVariationPayout> calculatePayouts() {
		final BigDecimal total = calculateTotalPrize();
		int rank = 0;
		for (BigDecimal amount : payoutAmounts) {
			BigDecimal payout = amount.divide(total);
			payouts.add(new TournamentVariationPayout(++rank, payout));
		}
		return payouts;
	}

}
