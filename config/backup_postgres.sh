#!/bin/bash
D=`date +%Y-%m-%d_%H-%M-%S`
######## Some parameters.
BACK_DIR_J="/BackupJira"
BACK_DIR_C="/BackupWiki"
#
DIR_JIRA="/opt/jira"
DIR_J_HOME="/opt/application-data/jira"
#
DIR_C="/opt/confluence"
DIR_C_HOME="/opt/application-data/confluence"
########
/bin/mkdir  $BACK_DIR_J/db

pg_dump --host 127.0.0.1 --port 5432 --username postgres --format=c --blobs --quote-all-identifiers --verbose --file /BackupJira/db/$D-jira.psql jira
/bin/tar -czpf $BACK_DIR_J/$D-jira.psql.tar.gz $BACK_DIR_J/db/$D-jira.psql

/bin/tar --exclude=$DIR_J_HOME/log --exclude=$DIR_J_HOME/plugins/.osgi-plugins -czpf $BACK_DIR_J/$D-Jira-home.tar.gz $DIR_J_HOME
/bin/tar --exclude=$DIR_JIRA/logs --exclude=$DIR_JIRA/temp -czpf $BACK_DIR_J/$D-Jira-opt.tar.gz $DIR_JIRA

/bin/rm -rf $BACK_DIR_J/db/*
/bin/rm -rf $BACK_DIR_J/db

########
/bin/mkdir  $BACK_DIR_C/db

pg_dump --host 127.0.0.1 --port 5432 --username postgres --format=c --blobs --quote-all-identifiers --verbose --file /BackupWiki/db/$D-confluence.psql confluence
/bin/tar -czpf $BACK_DIR_C/$D-confluence.psql.tar.gz $BACK_DIR_C/db/$D-confluence.psql

/bin/tar --exclude=$DIR_C_HOME/logs --exclude=$DIR_C_HOME/plugins-osgi-cache -czpf $BACK_DIR_C/$D-confluence-opt.tar.gz $DIR_C_HOME
/bin/tar --exclude=$DIR_C/logs --exclude=$DIR_C/temp -czpf $BACK_DIR_C/$D-confluence-home.tar.gz $DIR_C

/bin/rm -rf $BACK_DIR_C/db/*
/bin/rm -rf $BACK_DIR_C/db