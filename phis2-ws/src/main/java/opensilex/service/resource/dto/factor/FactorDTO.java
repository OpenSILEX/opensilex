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
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;

/**
 * Factor DTO.
 * @author arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class FactorDTO extends RdfResourceDefinitionDTO{
    
    //comment of the rdf resource
    protected String comment;
    
    //ontologies references of the rdf resource
    protected ArrayList<OntologyReference> ontologiesReferences;

    public FactorDTO(Factor factor) {
        super(factor);
        this.comment = factor.getComment();
        ontologiesReferences = factor.getOntologiesReferences();
    }

    
    
}