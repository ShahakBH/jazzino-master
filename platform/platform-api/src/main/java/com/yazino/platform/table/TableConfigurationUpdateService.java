package com.yazino.platform.table;


import java.io.IOException;

public interface TableConfigurationUpdateService {

    void setAvailabilityFor(String gameType,
                            boolean available);

    void asyncSetAvailabilityFor(String gameType,
                                 boolean available);

    void disableAndShutdownAllGames();

    void asyncDisableAndShutdownAllGames();

    void publishCountdownForAllGames(Long countdown);

    void asyncPublishCountdownForAllGames(Long countdown);

    void publishCountdownForGameType(final Long countdownTimeout, String gameType);

    void asyncPublishCountdownForGameType(final Long countdownTimeout, String gameType);

    void stopCountdown(String countdownId);

    void asyncStopCountdown(String countdownId);

    void refreshTemplates();

    void asyncRefreshTemplates();

    void refreshGameConfigurations();

    void asyncRefreshGameConfigurations();

    /**
     * Uses OSGI to dynamically replace the server side logic for a game.
     *
     * @param filename the file name of the game JAR
     * @throws IOException thrown if there is a problem creating the OSGI bundle
     */
    void publishGame(String filename) throws IOException;

}
