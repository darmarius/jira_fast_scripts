#!/bin/bash
D=`date +%Y-%m-%d_%H-%M-%S`
######## Some parameters.
BACK_DIR="/s3mnt"
#
MYSQL_AUTH_J="-ujira -p"password" -hlocalhost"
MYSQL_BASE_J="jira"
DIR_JIRA="/opt/jira/"
DIR_J_HOME="/opt/application-data/jira/"
#
########

/bin/mkdir  $BACK_DIR/db

/usr/bin/mysqldump $MYSQL_AUTH_J --databases $MYSQL_BASE_J > $BACK_DIR/db/$D-jira.sql
/bin/tar -czpf $BACK_DIR/$D-Jira_db.tar.gz $BACK_DIR/db/
/bin/tar --exclude=$DIR_J_HOME/log --exclude=$DIR_J_HOME/plugins/.osgi-plugins -czpf $BACK_DIR/$D-Jira-home.tar.gz $DIR_J_HOME
/bin/tar --exclude=$DIR_JIRA/jre --exclude=$DIR_JIRA/logs --exclude=$DIR_JIRA/temp -czpf $BACK_DIR/$D-Jira-opt.tar.gz $DIR_JIRA

/bin/rm -rf $BACK_DIR/db/*
/bin/rm -rf $BACK_DIR/db