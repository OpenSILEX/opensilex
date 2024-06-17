#!/bin/bash

#
# ******************************************************************************
#                         start_opensilex.sh
# OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
# Copyright © INRAE 2024.
# Last Modification: 17/06/2024 16:32
# Contact: gabriel.besombes@inrae.fr
# ******************************************************************************
#

# Redirect all subsequent commands' stdout and stderr to the terminal
exec > >(tee) 2>&1

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

pwd

cd "$SCRIPT_DIR" || exit 1
CONFIG_FILE="$(readlink -f "opensilex.yml")"

pwd

cd "${SCRIPT_DIR}/../../../opensilex-dev-tools/src/main/resources/docker" || exit 1

pwd

docker compose -p test up &

sleep 30

cd "${SCRIPT_DIR}/../../../../../opensilex-release/target/opensilex" || exit 1

OPENSILEX="java -jar opensilex.jar"

pwd

$OPENSILEX system install --CONFIG_FILE="$CONFIG_FILE" &&
$OPENSILEX --CONFIG_FILE="$CONFIG_FILE" user add --admin &&
$OPENSILEX server start --CONFIG_FILE="$CONFIG_FILE" --port=8080 --adminPort=4080 --DEBUG