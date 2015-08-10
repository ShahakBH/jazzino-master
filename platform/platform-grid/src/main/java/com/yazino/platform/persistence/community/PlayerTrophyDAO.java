package com.yazino.platform.persistence.community;

import com.yazino.platform.model.community.PlayerTrophy;

/**
 * Describes a class able to provide access to Player Trophy data.
 */
public interface PlayerTrophyDAO {

    /**
     * Inserts the specified player's trophy.
     *
     * @param playerTrophy never null
     */
    void insert(PlayerTrophy playerTrophy);

    PlayerTrophyDAO NULL = new PlayerTrophyDAO() {

        @Override
        public void insert(final PlayerTrophy playerTrophy) {

        }

        @Override
        public String toString() {
            return PlayerTrophyDAO.class.getName().concat(".NULL");
        }
    };

}
