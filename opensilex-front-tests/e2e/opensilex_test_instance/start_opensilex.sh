#!/bin/bash

#
# ******************************************************************************
#                         start_opensilex.sh
# OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
# Copyright © INRAE 2024.
# Last Modification: 21/06/2024 13:40
# Contact: gabriel.besombes@inrae.fr
# ******************************************************************************
#

# Redirect all subsequent commands' stdout and stderr to the terminal
exec > >(tee) 2>&1

# Get dockerisedBases flag
dockerisedBases=0

while getopts ":d" opt; do
  case "$opt" in
    d) dockerisedBases=1 ;;
    \?) echo "Invalid option -$OPTARG" >&2 ;;
  esac
done

# Get absolute path of this script's directory
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

# Get the config file
CONFIG_FILE="${SCRIPT_DIR}/../../../opensilex-dev-tools/src/main/resources/config/opensilex.yml"


# Start the dockers if needed
if [ "$dockerisedBases" = 1 ]; then
  cd "${SCRIPT_DIR}/../../../opensilex-dev-tools/src/main/resources/docker" || exit 1
  if ! docker ps | grep "opensilex-mongodb\|opensilex-rdf4j"
    then
      docker compose -p test up --quiet-pull & # --quiet-pull for less output
      sleep 30
  fi
fi

# Run maven clean install to get the latest changes
# -DskipTests to skip backend tests
# -q for quiet (only show errors)
cd "${SCRIPT_DIR}/../../.." || exit 1
mvn clean install -DskipTests -q &&

# Install and start OpenSILEX
cd "${SCRIPT_DIR}/../../../opensilex-release/target/opensilex" || exit 1
OPENSILEX="java -jar opensilex.jar"
$OPENSILEX system install --CONFIG_FILE="$CONFIG_FILE" &&
$OPENSILEX --CONFIG_FILE="$CONFIG_FILE" user add --admin &&
$OPENSILEX server start --CONFIG_FILE="$CONFIG_FILE" --port=8080 --adminPort=4080