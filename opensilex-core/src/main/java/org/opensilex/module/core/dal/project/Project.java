/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.dal.project;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.module.core.ontology.Oeso;
import org.opensilex.module.core.service.sparql.annotations.SPARQLProperty;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResource;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResourceURI;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@SPARQLResource(
        ontology = FOAF.class,
        resource = "Project"
)
public class Project {
    @SPARQLResourceURI()
    private URI uri;
    
    @SPARQLProperty(
            ontology = FOAF.class,
            property = "name"
    )
    private String name;
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasRelatedProject"
    )
    private List<Project> relatedProject = new ArrayList<>();
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasRelatedProject"
    )
    private String shortname;
//    private String financialSupport;
//    private String financialName;
//    private Date dateStart;
//    private Date dateEnd;
//    private List<String> keywords = new ArrayList<>();
//    private String description;
//    private String objective;
//    private URL website;
//    
//    private List<ContactDTO> scientificContacts = new ArrayList<>();
//    private List<ContactDTO> administrativeContacts = new ArrayList<>();
//    private List<ContactDTO> coordinatorContacts = new ArrayList<>();
}
