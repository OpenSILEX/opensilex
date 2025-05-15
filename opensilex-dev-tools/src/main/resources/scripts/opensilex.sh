#!/bin/bash

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

cd $SCRIPT_DIR

java -jar --add-opens java.base/java.io=ALL-UNNAMED opensilex.jar "$@"
