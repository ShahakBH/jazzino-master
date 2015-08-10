DROP EVENT IF EXISTS evtExtractAccountActivity#
CREATE EVENT evtExtractAccountActivity 
ON SCHEDULE EVERY 1 MINUTE 
DO CALL extractAccountActivity()#
