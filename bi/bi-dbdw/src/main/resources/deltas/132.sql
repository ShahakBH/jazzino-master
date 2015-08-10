DROP TRIGGER IF EXISTS account_platform_fix#
CREATE TRIGGER account_platform_fix
BEFORE INSERT ON ACCOUNT_SESSION
FOR EACH ROW
BEGIN
	IF INSTR(NEW.START_PAGE,'mobile') AND NEW.PLATFORM <> 'MOBILE_ANDROID' AND NEW.PLATFORM <> 'ANDROID' THEN
		SET NEW.PLATFORM = 'MOBILE';
	END IF;
END
#