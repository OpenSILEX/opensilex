//******************************************************************************
//                                FactorDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 12 août 2019
// Contact:arnaud.charleroy, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.factor;

import java.util.ArrayList;
import opensilex.service.model.Factor;
import opensilex.service.model.OntologyReference;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDTO;

/**
 * Factor DTO.
 * @author arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class FactorDTO extends RdfResourceDTO{
    
    //comment of the rdf resource
    protected String comment;
    
    //ontologies references of the rdf resource
    protected ArrayList<OntologyReference> ontologiesReferences;

    public FactorDTO(Factor factor) {
        super(factor);
        this.comment = factor.getComment();
        ontologiesReferences = factor.getOntologiesReferences();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ArrayList<OntologyReference> getOntologiesReferences() {
        return ontologiesReferences;
    }

    public void setOntologiesReferences(ArrayList<OntologyReference> ontologiesReferences) {
        this.ontologiesReferences = ontologiesReferences;
    }

    
    
    
}