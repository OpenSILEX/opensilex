#!/bin/bash

#
# ******************************************************************************
#                         start_opensilex.sh
# OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
# Copyright © INRAE 2024.
# Last Modification: 18/06/2024 15:59
# Contact: gabriel.besombes@inrae.fr
# ******************************************************************************
#

# Redirect all subsequent commands' stdout and stderr to the terminal
exec > >(tee) 2>&1

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

cd "$SCRIPT_DIR" || exit 1
CONFIG_FILE="$(readlink -f "opensilex.yml")"

cd "${SCRIPT_DIR}/../../.." || exit 1
mvn clean install -DskipTests

cd "${SCRIPT_DIR}/../../../opensilex-dev-tools/src/main/resources/docker" || exit 1

if ! docker ps | grep "opensilex-mongodb\|opensilex-rdf4j"
  then
    docker compose -p test up &
    sleep 30
fi

cd "${SCRIPT_DIR}/../../../opensilex-release/target/opensilex" || exit 1

OPENSILEX="java -jar opensilex.jar"

$OPENSILEX system install --CONFIG_FILE="$CONFIG_FILE" &&
$OPENSILEX --CONFIG_FILE="$CONFIG_FILE" user add --admin &&
$OPENSILEX server start --CONFIG_FILE="$CONFIG_FILE" --port=8080 --adminPort=4080 --DEBUG