//******************************************************************************
//                             InstanceDefinitonDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 16 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import opensilex.service.resources.validation.interfaces.Required;
import opensilex.service.resources.dto.manager.AbstractVerifiedClass;
import opensilex.service.resources.validation.interfaces.URL;
import opensilex.service.model.RdfResourceDefinition;
import opensilex.service.model.OntologyReference;

/**
 * A class which contains methods to automatically check the attributes of a 
 * class, from rules defined by user.
 * Contains the list of the elements which might be send by the client to save 
 * the database.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class InstanceDefinitionDTO extends AbstractVerifiedClass {
    //SILEX:info
    //Pour l'instant, on ne prend qu'un label et un comment. 
    //Il faudra pouvoir en avoir plusieurs (ex. labels par langue).
    //Il faudra donc modifier les req d'insertion/recherche/suppression/modification
    //\SILEX:info
    protected String uri;
    protected String label;
    protected String comment;
    protected ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();

    @Override
    public RdfResourceDefinition createObjectFromDTO() {
        RdfResourceDefinition instanceDefinition = new RdfResourceDefinition();
        instanceDefinition.setUri(uri);
        instanceDefinition.setLabel(label);
        instanceDefinition.setComment(comment);
        
        if (ontologiesReferences != null  && !ontologiesReferences.isEmpty()) {
            for (OntologyReference ontologyReference : ontologiesReferences) {
                instanceDefinition.addOntologyReference(ontologyReference);
            }
        }
        
        return instanceDefinition;
    }

    @URL
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/id/variables/v001")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    @Required
    @ApiModelProperty(example = "LAI")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ApiModelProperty(example = "comment")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    
    //SILEX:todo
    // Do the DTO OntologyReferenceDTO
    //\SILEX:todo
    @Valid
    public ArrayList<OntologyReference> getOntologiesReferences() {
        return ontologiesReferences;
    }

    public void setOntologiesReferences(ArrayList<OntologyReference> ontologiesReferences) {
        this.ontologiesReferences = ontologiesReferences;
    }
}
