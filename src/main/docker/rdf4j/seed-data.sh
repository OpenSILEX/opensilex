echo -e "connect http://rdf4j:8080/rdf4j-server \n\
create native \n\
phis \n\
phis \n\n\n\n\
open phis \n\
load /tmp/oeso.owl into urn:http://www.opensilex.org/vocabulary/oeso \n\
load /tmp/oeev.owl into urn:http://www.opensilex.org/vocabulary/oeev \n\
load /tmp/oa.rdf into urn:http://www.w3.org/ns/oa \n\
exit" | /tmp/eclipse-rdf4j-2.5.2/bin/console.sh