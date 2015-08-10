CREATE OR REPLACE VIEW player AS
  SELECT
    P.PLAYER_ID AS PLAYER_ID,
    P.ACCOUNT_ID AS ACCOUNT_ID,
    A.balance,
    P.CREATED_TS AS registration_date,
    ref.registration_platform,
    ref.registration_game_type,
    U.picture_location,
    U.country
  FROM PLAYER_DEFINITION P LEFT join LOBBY_USER U on P.PLAYER_ID = U.PLAYER_ID
    LEFT JOIN ACCOUNT A on A.account_id = P.account_id
    LEFT JOIN PLAYER_REFERRER ref on ref.player_id = p.player_id;
