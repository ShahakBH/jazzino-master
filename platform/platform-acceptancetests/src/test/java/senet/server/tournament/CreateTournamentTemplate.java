package senet.server.tournament;

import com.yazino.platform.tournament.TournamentType;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.platform.model.tournament.TournamentVariationTemplateBuilder;
import senet.server.WiredSetUpFixture;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

public class CreateTournamentTemplate extends WiredSetUpFixture {
	@Autowired(required = true)
    @Qualifier("jdbcTemplate")
	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(final JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private TournamentVariationTemplateBuilder commonSetup(String name, String gameType, String tournamentType) {
		TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder =  new TournamentVariationTemplateBuilder(jdbcTemplate);
		tournamentVariationTemplateBuilder.setTemplateName(name);
		tournamentVariationTemplateBuilder.setGameType(gameType);
		tournamentVariationTemplateBuilder.setTournamentType(TournamentType.valueOf(tournamentType));
		return tournamentVariationTemplateBuilder;
	}

	public void templateNameGameTypeTournamentType(String name, String gameType, String tournamentType) {
		TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder = commonSetup(name, gameType, tournamentType);
		TournamentFixture.saveTemplate(tournamentVariationTemplateBuilder.saveToDatabase());
	}

	public void templateNameGameTypeTournamentTypeEntryFeeServiceFeeMaxPlayers(
			String name,
			String gameType,
			String tournamentType,
			String entryFee,
			String serviceFee,
			Integer maxPlayers) {
		TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder = commonSetup(name, gameType, tournamentType);
		tournamentVariationTemplateBuilder.setEntryFee(new BigDecimal(entryFee));
		tournamentVariationTemplateBuilder.setServiceFee(new BigDecimal(serviceFee));
		tournamentVariationTemplateBuilder.setMaxPlayers(maxPlayers);
		TournamentFixture.saveTemplate(tournamentVariationTemplateBuilder.saveToDatabase());
	}

	public void templateNameGameTypeTournamentTypeEntryFeeServiceFeeMinPlayers(
			String name,
			String gameType,
			String tournamentType,
			String entryFee,
			String serviceFee,
			Integer minPlayers) {
		TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder = commonSetup(name, gameType, tournamentType);
		tournamentVariationTemplateBuilder.setEntryFee(new BigDecimal(entryFee));
		tournamentVariationTemplateBuilder.setServiceFee(new BigDecimal(serviceFee));
		tournamentVariationTemplateBuilder.setMinPlayers(minPlayers);
		TournamentFixture.saveTemplate(tournamentVariationTemplateBuilder.saveToDatabase());
	}

	public void templateNameGameTypeTournamentTypeMaxPlayers(
			String name,
			String gameType,
			String tournamentType,
			Integer maxPlayers) {
		TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder = commonSetup(name, gameType, tournamentType);
		tournamentVariationTemplateBuilder.setMaxPlayers(maxPlayers);
		TournamentFixture.saveTemplate(tournamentVariationTemplateBuilder.saveToDatabase());
	}

	public void templateNameGameTypeTournamentTypeMinPlayers(
			String name,
			String gameType,
			String tournamentType,
			Integer minPlayers) {
		TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder = commonSetup(name, gameType, tournamentType);
		tournamentVariationTemplateBuilder.setMinPlayers(minPlayers);
		TournamentFixture.saveTemplate(tournamentVariationTemplateBuilder.saveToDatabase());
	}

	public void templateNameGameTypeTournamentTypeStartingChips(
			String name,
			String gameType,
			String tournamentType,
			String startingChips) {
		TournamentVariationTemplateBuilder tournamentVariationTemplateBuilder = commonSetup(name, gameType, tournamentType);
		tournamentVariationTemplateBuilder.setStartingChips(new BigDecimal(startingChips));
		TournamentFixture.saveTemplate(tournamentVariationTemplateBuilder.saveToDatabase());
	}
}
