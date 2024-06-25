#!/bin/bash

#
# ******************************************************************************
#                         start_opensilex.sh
# OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
# Copyright © INRAE 2024.
# Last Modification: 25/06/2024 15:09
# Contact: gabriel.besombes@inrae.fr
# ******************************************************************************
#

# Redirect all subsequent commands' stdout and stderr to the terminal
exec > >(tee) 2>&1

# Get dockerisedBases flag
dockerisedBases=0

# Build flag
mvnBuild=0

for arg in "$@"; do
  case $arg in
    -d)
      dockerisedBases=1
      echo "Option -d detected, dockerised bases will be started if needed" >&2
      shift # Remove the processed argument from the list
      ;;
    -b)
      mvnBuild=1
      echo "Option -b detected, maven build wil be executed before installing OpenSILEX" >&2
      shift # Remove the processed argument from the list
      ;;
    *)
      echo "Invalid option $arg" >&2
      ;;
  esac
done

# Get absolute path of this script's directory
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

# Get the config file
CONFIG_FILE="${SCRIPT_DIR}/../../../opensilex-dev-tools/src/main/resources/config/opensilex.yml"

# Start the dockers if needed
if [ "$dockerisedBases" = 1 ]; then
  cd "${SCRIPT_DIR}/../../../opensilex-dev-tools/src/main/resources/docker" || exit 1
  if ! docker ps | grep "opensilex-mongodb\|opensilex-rdf4j" >> "${SCRIPT_DIR}/logs/docker_ps.log" 2>&1
    then
      echo "===========STARTING DOCKERIZED BASES==========="
      dockerd &
      sleep 5
      docker compose -p test up >> "${SCRIPT_DIR}/logs/docker_compose.log" 2>&1 & # --quiet-pull for less output
      sleep 30
  fi
fi

# Run maven clean install to get the latest changes
# -DskipTests to skip backend tests
# -q for quiet (only show errors)
if [ "$mvnBuild" = 1 ]; then
  cd "${SCRIPT_DIR}/../../.." || exit 1
  echo "===========MVN CLEAN INSTALL==========="
  mvn clean install -DskipTests -X >> "${SCRIPT_DIR}/logs/mvn_clean_install.log" 2>&1
fi

# Install and start OpenSILEX
cd "${SCRIPT_DIR}/../../../opensilex-release/target/opensilex" || exit 1
OPENSILEX="java -jar opensilex.jar"
echo "===========INSTALLING OPENSILEX==========="
$OPENSILEX system install --CONFIG_FILE="$CONFIG_FILE" >> "${SCRIPT_DIR}/logs/opensilex_system_install.log" 2>&1 &&
echo "===========ADDING ADMIN==========="
$OPENSILEX --CONFIG_FILE="$CONFIG_FILE" user add --admin >> "${SCRIPT_DIR}/logs/opensilex_user_add.log" 2>&1 &&
echo "===========STARTING OPENSILEX==========="
$OPENSILEX server start --CONFIG_FILE="$CONFIG_FILE" --port=8080 --adminPort=4080 --DEBUG >> "${SCRIPT_DIR}/logs/opensilex_server_start.log" 2>&1