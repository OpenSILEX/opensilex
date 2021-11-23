
 
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  27 April, 2020
 *  Contact: arnaud.charleroy@inrae.fr
 

Create a new module (Alpha version)
==============

Prerequisites:

- For Unix system
- Need ``npm`` javascript library management

Steps :

- Install mustache : ``npm install -g mustache``

- Extract ``template_config.zip`` to ``template_config``

- Go to ``opensilex-dev-tools/src/main/resources/module/skeleton`` folder

- Execute ``chmod +u generateModule.sh`` and run ``./generateModule.sh organisation module`` script
Where organisation => inrae or ifv or google and module is module name exemple sinfonia, sixtine other