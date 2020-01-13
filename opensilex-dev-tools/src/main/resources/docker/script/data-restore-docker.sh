#!/bin/bash
###############################################################################
##                        data-restore.sh
## SILEX-PHIS
## Copyright © INRA 2019
## Creation date: 11 january, 2019
## Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
###############################################################################

# This script restore a previous backup of all data in PostGreSQL, MongoDB and RDF4J

# Local variable to configure

RDF4J_PATH=/home/bonnefoj/Documents/TOOLS/eclipse-rdf4j-2.5.3

JAVA_PATH=/usr/lib/jvm/java-8-openjdk-amd64/

BACKUP_DIRECTORY=/home/bonnefoj/Documents/opensilex_data

HOST=localhost

INSTANCE=phis

PG_USER=opensilex

PG_PASS=azerty

PG_HOST=$HOST

PG_DB=expe

SQL_DUMP_PATH=$BACKUP_DIRECTORY/pg_backup.sql

RDF4J_HOST=$HOST

RDF4J_REPOSITORY=diaphen

RDF4J_DUMP_PATH=$BACKUP_DIRECTORY/rdf4j_backup.nq

MONGO_HOST=$HOST

MONGO_DB=phis

MONGO_DUMP_PATH=$BACKUP_DIRECTORY/mongo_backup

###############################################################################
export PATH=$JAVA_PATH:$PATH

function restorePgSQL() {
    echo "[RESTORE] PostgreSQL"
    export PGPASSWORD="$PG_PASS"
    psql -h $PG_HOST -U $PG_USER -d postgres -c "
        SELECT pg_terminate_backend(pg_stat_activity.pid)
        FROM pg_stat_activity
        WHERE pg_stat_activity.datname = '$PG_DB'
        AND pid <> pg_backend_pid();"
    psql -h $PG_HOST -U $PG_USER -d postgres -c "DROP DATABASE IF EXISTS $PG_DB;"
    psql -h $PG_HOST -U $PG_USER -d postgres -c "CREATE DATABASE $PG_DB WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'fr_FR.UTF-8' LC_CTYPE = 'fr_FR.UTF-8';"
    psql -h $PG_HOST -U $PG_USER -d postgres -c "ALTER DATABASE $PG_DB OWNER TO $PG_USER;"
    psql -h $PG_HOST -U $PG_USER -d $PG_DB -f $SQL_DUMP_PATH
}

function restoreTriplestore() {
    echo "[RESTORE] RDF4J"
    echo -e "open $RDF4J_REPOSITORY
    clear
    load $RDF4J_DUMP_PATH
    exit" | $RDF4J_PATH/bin/console.sh -s http://$RDF4J_HOST:8080/rdf4j-server
    echo "\n"
}

function restoreMongo() {
    echo "[RESTORE] MongoDB"
    mongo $MONGO_HOST/$MONGO_DB --eval "db.dropDatabase()"
    mongorestore -h $MONGO_HOST -d $MONGO_DB $MONGO_DUMP_PATH/$MONGO_DB
}

if [ -d $BACKUP_DIRECTORY ]; then
    
    restorePgSQL

    restoreTriplestore

    restoreMongo
fi

