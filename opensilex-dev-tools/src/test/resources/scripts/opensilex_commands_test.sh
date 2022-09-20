# Title : OPENSILEX_CMD_commands_test.sh
# Description : Script used to test commands with a OpenSILEX jar archive
# Date : 20/09/2022
# Authors : Renaud COLIN, Guilhem HEINRICH

# Check input argument
if [[ $# -lt 4 ]] ; then
    echo -e "\e[31mRTFM : not enough argument provided, 4 arguments expected \e[0m"
    echo -e "\e[32mGood usage : opensilex_commands_test.sh <OPENSILEX_HOME_DIRECTORY> <OPENSILEX_CONFIG_FILE> <DEFAULT_ADMIN_LOGIN> <DEFAULT_ADMIN_PWD> \e[0m"

    echo "OPENSILEX_HOME_DIRECTORY : absolute path to the directory which contains your opensilex.jar archive and modules directory"
    echo "OPENSILEX_HOME_DIRECTORY : absolute path to your opensilex yaml (.yml) config"
    echo "DEFAULT_ADMIN_LOGIN : email address of your default admin user"
    echo "DEFAULT_ADMIN_PWD : password of your default admin user"

    echo -e "\e[32mExample : opensilex_commands_test.sh /home/user/opensilex/ /home/user/opensilex/opensilex.yml random_user_45@opensilex.fr Nf65F8eclrMnolapEAvN2tAPkz52d6sN \e[0m"
    exit 1
fi

# User variables
OPENSILEX_HOME=$1
OPENSILEX_CONFIG_FILE=$2
DEFAULT_ADMIN_LOGIN=$3
DEFAULT_ADMIN_PWD=$4

OPENSILEX_CMD="java -jar ${OPENSILEX_HOME}/opensilex.jar"
OPENSILEX_CONFIG="--CONFIG_FILE=${OPENSILEX_CONFIG_FILE}"

cd "$OPENSILEX_HOME" || exit;

# Display the full configuration that will be effectively used by OpenSILEX
# (display input config + default config values)
$OPENSILEX_CMD system full-config "$OPENSILEX_CONFIG" || exit;

# System install -> create RDF4J repository and MongoDB database
$OPENSILEX_CMD system install "$OPENSILEX_CONFIG" || exit;

# Create the default admin user (use default login/password in no input was provided for this script)
$OPENSILEX_CMD user add --admin --email="$DEFAULT_ADMIN_LOGIN" --password="$DEFAULT_ADMIN_PWD" "$OPENSILEX_CONFIG" || exit;

# Ensure that all ontologies are up to date
# (this is ok after install, but here we want to ensure that command is OK)
$OPENSILEX_CMD sparql reset-ontologies "$OPENSILEX_CONFIG"  || exit;

# Check that RDF4J repository and MongoDB database creation, test RDF4J ontologies integrity
# and ensure that at least one user with password exists in RDF4J
$OPENSILEX_CMD system check "$OPENSILEX_CONFIG"  || exit;

# Run server on port 8081 with DEBUG in background
$OPENSILEX_CMD server start --port=8081 --adminPort=4081 --DEBUG "$OPENSILEX_CONFIG" &

# Sleep 1m until the server is well started
# "sleep 1m"

# Stop server on port 8081 with DEBUG
$OPENSILEX_CMD server stop --port=8081 --adminPort=4081 --DEBUG "$OPENSILEX_CONFIG" || exit;

