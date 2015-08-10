package senet.server.tournament;

import org.springframework.beans.factory.annotation.Qualifier;
import senet.server.WiredSetUpFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yazino.platform.tournament.TournamentException;
import com.yazino.platform.tournament.TournamentVariationPayout;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import com.yazino.platform.model.tournament.TournamentVariationTemplateBuilder;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class CreateTournamentPayoutForTournamentTemplate extends WiredSetUpFixture {
    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(final JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public CreateTournamentPayoutForTournamentTemplate() {
	}

    public void templateNameRankPrizeAsPercentage(final String templateName,
                        final String rank,
                        final String prizeAsPercentage)
			throws TournamentException {
		notNull(templateName, "Template name must be set");
        notNull(rank, "Rank name must be set");
		notNull(prizeAsPercentage, "Prize percentage must be set");

		TournamentVariationTemplate tournamentVariationTemplate = TournamentFixture.getTemplateByName(templateName);

        TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder = new TournamentVariationTemplateBuilder(jdbcTemplate, tournamentVariationTemplate);

        TournamentVariationPayout tournamentVariationPayout = new TournamentVariationPayout(Integer.parseInt(rank), convertPercentToQuotient(new BigDecimal(prizeAsPercentage)));

        tournamentVariationTemplateBuilder.addTournamentPayout(tournamentVariationPayout);

        TournamentFixture.saveTemplate(tournamentVariationTemplateBuilder.saveToDatabase());
	}

    private BigDecimal convertPercentToQuotient(final BigDecimal prizeAsPercentage) {
        if (prizeAsPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return prizeAsPercentage.divide(new BigDecimal(100L));
    }

}
