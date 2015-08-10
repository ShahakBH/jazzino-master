-- WEB-3678 - Index ACCOUNT_IDs since we lose this via dropping the foreign key

ALTER TABLE ACCOUNT_STATEMENT ADD INDEX IDX_ACCT_STMT_ACCT_ID (ACCOUNT_ID)#
