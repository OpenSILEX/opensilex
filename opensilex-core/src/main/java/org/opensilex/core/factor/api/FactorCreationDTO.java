/*
 * ******************************************************************************
 *                                     FactorCreationDTO.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorCreationDTO extends SKOSReferencesDTO {

    @ValidURI
    private URI uri;
    
    @Required
    private String name;

    private String category;

    private String comment;

    @Valid
    private List<FactorLevelCreationDTO> factorLevels;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    } 

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<FactorLevelCreationDTO> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelCreationDTO> factorsLevels) {
        this.factorLevels = factorsLevels;
    }

    public FactorModel newModel() {
        FactorModel model = new FactorModel();
        model.setUri(getUri()); 
        model.setName(getName());
        model.setCategory(getCategory());
        model.setComment(getComment());
        this.setSkosReferencesToModel(model);

        return model;
    }
}
