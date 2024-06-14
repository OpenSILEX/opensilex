#!/bin/bash

#
# ******************************************************************************
#                         start_opensilex.sh
# OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
# Copyright © INRAE 2024.
# Last Modification: 14/06/2024 12:41
# Contact: gabriel.besombes@inrae.fr
# ******************************************************************************
#

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

cd "$SCRIPT_DIR" || exit 1
CONFIG_FILE="$(readlink -f "opensilex.yml")"

cd "${SCRIPT_DIR}/../../../opensilex-release/target/opensilex" || exit 1

OPENSILEX="java -jar opensilex.jar"

ls

$OPENSILEX system install --CONFIG_FILE="$CONFIG_FILE" &&
$OPENSILEX --CONFIG_FILE="$CONFIG_FILE" user add --admin &&
$OPENSILEX server start --CONFIG_FILE="$CONFIG_FILE" --port=8080 --adminPort=4080 --DEBUG