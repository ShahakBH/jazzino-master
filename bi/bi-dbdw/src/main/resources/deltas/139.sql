DROP FUNCTION IF EXISTS inferRevenueFromDefaultChips#

CREATE FUNCTION inferRevenueFromDefaultChips(DEFAULT_CHIPS INT) RETURNS INT DETERMINISTIC
BEGIN
    CASE DEFAULT_CHIPS
        WHEN 10000 THEN
            RETURN 5;
        WHEN 21000 THEN
            RETURN 10;
        WHEN 50000 THEN
            RETURN 15;
        WHEN 150000 THEN
            RETURN 20;
        WHEN 400000 THEN
            RETURN 50;
        WHEN 1000000 THEN
            RETURN 150;
        WHEN 5000 THEN
            RETURN 3;
        WHEN 15000 THEN
            RETURN 8;
        WHEN 30000 THEN
            RETURN 15;
        WHEN 70000 THEN
            RETURN 30;
        WHEN 200000 THEN
            RETURN 70;
        WHEN 300000 THEN
            RETURN 90;
        ELSE
            BEGIN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = "Unrecognized payment option.";
            END;
    END CASE;
END;
#
