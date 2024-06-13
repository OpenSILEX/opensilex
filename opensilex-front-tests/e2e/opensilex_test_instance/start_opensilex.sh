#!/bin/bash

#
# ******************************************************************************
#                         start_opensilex.sh
# OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
# Copyright © INRAE 2024.
# Last Modification: 13/06/2024 20:51
# Contact: gabriel.besombes@inrae.fr
# ******************************************************************************
#

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

cd "$SCRIPT_DIR" || exit

OPENSILEX="${SCRIPT_DIR}/../../../opensilex-release/target/opensilex/opensilex.sh"
CONFIG_FILE="$(readlink -f "opensilex.yml")"

$OPENSILEX system install --CONFIG_FILE="$CONFIG_FILE" &&
$OPENSILEX user add --admin --CONFIG_FILE="$CONFIG_FILE" &&
$OPENSILEX server start --CONFIG_FILE="$CONFIG_FILE" --port=8080 --adminPort=4080 --DEBUG