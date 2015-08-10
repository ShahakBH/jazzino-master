#!/bin/bash
psql -dtemplate1 <<EOF
drop database reporting;
drop role SCHEMA_MANAGER;
drop role READ_WRITE;
drop role READ_ONLY;
drop user reporting;
drop user readwrite;
drop user readonly;
drop user admin;
drop role SUPER_USER;
EOF
