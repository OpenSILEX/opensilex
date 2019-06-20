FROM eclipse/rdf4j-workbench:amd64-latest

USER root
RUN  apk update \                                                                                                                                                                                                                        
  &&  apk add ca-certificates wget \                                                                                                                                                                                                      
  &&  update-ca-certificates  

USER tomcat
WORKDIR /tmp
RUN wget "http://www.w3.org/ns/oa.rdf"
RUN wget "https://raw.githubusercontent.com/OpenSILEX/ontology-vocabularies/master/oeso.owl"
RUN wget "https://raw.githubusercontent.com/OpenSILEX/ontology-vocabularies/master/oeev.owl" 

ADD seed-data.sh /tmp/seed-data.sh

USER root

RUN chown -R tomcat /var/rdf4j

USER tomcat