package senet.server.tournament;

import org.springframework.beans.factory.annotation.Qualifier;
import senet.server.WiredSetUpFixture;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yazino.platform.tournament.TournamentException;
import com.yazino.platform.tournament.TournamentVariationRound;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import com.yazino.platform.model.tournament.TournamentVariationTemplateBuilder;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class CreateTournamentLevelsForTournamentTemplate extends WiredSetUpFixture {
    
    private static final String SELECT_GAME_VARIATION_TEMPLATE_ID = "select GAME_VARIATION_TEMPLATE_ID " +
            "from GAME_VARIATION_TEMPLATE " +
            "WHERE GAME_TYPE = ? AND NAME = ?";

    @Autowired
    @Qualifier("jdbcTemplate")
	private JdbcTemplate jdbcTemplate;

    public CreateTournamentLevelsForTournamentTemplate() {
	}

    public void templateNameLevelGameVariationTemplateLengthInSecondsTimeUntilNextLevelInSeconds(final String templateName,
                        final Integer level,
                        final String gameVariationTemplate,
                        final Long lengthInSeconds,
                        final Long timeUntilNextLevelInSeconds)
			throws TournamentException {
		notBlank(templateName, "Template name must be set");
		notNull(level, "Level must be set");
        notBlank(gameVariationTemplate, "Game variation template must be set");
        notNull(lengthInSeconds, "Length in seconds must be set");
        notNull(timeUntilNextLevelInSeconds, "Timeout Until Next Level in seconds must be set");

		TournamentVariationTemplate tournamentVariationTemplate = TournamentFixture.getTemplateByName(templateName);

        TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder = new TournamentVariationTemplateBuilder(jdbcTemplate, tournamentVariationTemplate);

        BigDecimal gameVariationTemplateId = getGameVariationTemplateIdByName(tournamentVariationTemplate.getGameType(), gameVariationTemplate);

        TournamentVariationRound tournamentVariationRound = new TournamentVariationRound(level,
                        timeUntilNextLevelInSeconds,
                        lengthInSeconds,
                        gameVariationTemplateId,
                        "roger",
                        BigDecimal.valueOf(1L),
						"1");

        tournamentVariationTemplateBuilder.addTournamentRound(tournamentVariationRound);

        TournamentFixture.saveTemplate(tournamentVariationTemplateBuilder.saveToDatabase());
	}

    private BigDecimal getGameVariationTemplateIdByName(final String gameType, final String gameVariationTemplateName) {

        BigDecimal gameVariationTemplateId = BigDecimal.valueOf(jdbcTemplate.queryForLong(
				SELECT_GAME_VARIATION_TEMPLATE_ID,
				new Object[]{gameType, gameVariationTemplateName}
			));

        return gameVariationTemplateId;
    }
}
