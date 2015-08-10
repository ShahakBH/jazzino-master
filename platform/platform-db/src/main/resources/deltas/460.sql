-- WEB-4220 - advance sequence to avoid collisions with ACCOUNT_SESSION

INSERT INTO $SEQUENCE (ID, TSALLOCATED) VALUES (150000000, NOW())#
