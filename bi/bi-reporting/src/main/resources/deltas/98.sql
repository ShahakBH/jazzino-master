-- WEB-4242 - Store device information for client errors

ALTER TABLE CLIENT_LOG ADD COLUMN model character varying(32);
ALTER TABLE CLIENT_LOG ADD COLUMN api character varying(32);
ALTER TABLE CLIENT_LOG ADD COLUMN net character varying(32);