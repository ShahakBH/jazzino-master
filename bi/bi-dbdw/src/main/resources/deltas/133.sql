DROP EVENT IF EXISTS evtExtractAccountActivity#
CREATE EVENT evtExtractAccountActivity 
ON SCHEDULE EVERY 1 MINUTE 
COMMENT 'Fill the rpt_player_sources_mv materialized view'
DO CALL extractAccountActivity()#
